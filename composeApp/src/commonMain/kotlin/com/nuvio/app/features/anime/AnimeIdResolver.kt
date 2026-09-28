package com.nuvio.app.features.anime

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import io.ktor.http.encodeURLParameter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

interface AnimeIdResolver {
    suspend fun resolve(query: AnimeResolveQuery): AnimeResolution?
}

private val log = Logger.withTag("Anime")

/**
 * Resolves a Nuvio title to a MyAnimeList id using Tenrai's own search/catalog endpoint,
 * rather than a hardcoded anime database. Candidates are scored by title similarity (with
 * year, media-type and episode-count as tie-breakers) so that similarly named but distinct
 * shows - e.g. "Naruto" vs "Naruto Shippuden" vs "Boruto" - are not confused. A match below
 * [MIN_CONFIDENCE] is treated as unresolved rather than risking an incorrect match.
 */
class TenraiAnimeIdResolver : AnimeIdResolver {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun resolve(query: AnimeResolveQuery): AnimeResolution? {
        val candidates = search(query.title).ifEmpty {
            query.originalTitle?.takeIf { it.isNotBlank() }?.let { search(it) } ?: emptyList()
        }
        if (candidates.isEmpty()) return null

        val normalizedQueryTitles = buildSet {
            add(normalizeTitle(query.title))
            query.originalTitle?.let { add(normalizeTitle(it)) }
            query.alternativeTitles.forEach { add(normalizeTitle(it)) }
        }.filterTo(mutableSetOf()) { it.isNotBlank() }
        if (normalizedQueryTitles.isEmpty()) return null

        var bestCandidate: TenraiSearchAnimeDto? = null
        var bestScore = 0.0
        for (candidate in candidates) {
            val score = scoreCandidate(candidate, normalizedQueryTitles, query)
            if (score > bestScore) {
                bestScore = score
                bestCandidate = candidate
            }
        }

        val chosen = bestCandidate
        if (chosen == null || bestScore < MIN_CONFIDENCE) {
            log.d { "[Anime] no confident MAL match for '${query.title}' (best score=$bestScore)" }
            return null
        }

        log.d {
            "[Anime] MAL match selected malId=${chosen.malId} title='${chosen.title}' " +
                "confidence=$bestScore for query='${query.title}'"
        }
        return AnimeResolution(
            malId = chosen.malId,
            confidence = bestScore,
            matchedTitle = chosen.title ?: query.title,
        )
    }

    private suspend fun search(rawQuery: String): List<TenraiSearchAnimeDto> {
        val trimmed = rawQuery.trim()
        if (trimmed.isBlank()) return emptyList()
        return try {
            val url = "${TenraiConfig.BASE_URL}/anime?q=${trimmed.encodeURLParameter()}&limit=10"
            val text = httpGetText(url)
            json.decodeFromString(TenraiSearchResponse.serializer(), text).data
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            log.w { "[Anime] Tenrai search failed for '$trimmed': ${e.message}" }
            emptyList()
        }
    }

    private fun scoreCandidate(
        candidate: TenraiSearchAnimeDto,
        normalizedQueryTitles: Set<String>,
        query: AnimeResolveQuery,
    ): Double {
        val candidateTitles = buildSet {
            candidate.title?.let { add(it) }
            candidate.titleEnglish?.let { add(it) }
            candidate.titleJapanese?.let { add(it) }
            candidate.titleSynonyms.forEach { add(it) }
            candidate.titles.forEach { entry -> entry.title?.let { add(it) } }
        }.map(::normalizeTitle).filterTo(mutableSetOf()) { it.isNotBlank() }
        if (candidateTitles.isEmpty()) return 0.0

        var titleScore = 0.0
        for (queryTitle in normalizedQueryTitles) {
            for (candidateTitle in candidateTitles) {
                val similarity = titleSimilarity(queryTitle, candidateTitle)
                if (similarity > titleScore) titleScore = similarity
            }
        }
        if (titleScore <= 0.0) return 0.0

        var score = titleScore

        val candidateYear = candidate.aired?.prop?.from?.year
        if (query.year != null && candidateYear != null) {
            val diff = abs(query.year - candidateYear)
            score += when {
                diff == 0 -> 0.08
                diff <= 1 -> 0.02
                diff >= 3 -> -0.15
                else -> 0.0
            }
        }

        val expectedType = if (query.mediaType == "movie") "movie" else "tv"
        candidate.type?.let { type ->
            score += if (type.equals(expectedType, ignoreCase = true)) 0.05 else -0.05
        }

        val candidateEpisodes = candidate.episodes
        if (query.episodeCount != null && query.episodeCount > 0 && candidateEpisodes != null && candidateEpisodes > 0) {
            val ratio = min(query.episodeCount, candidateEpisodes).toDouble() /
                max(query.episodeCount, candidateEpisodes).toDouble()
            score += (ratio - 0.5) * 0.1
        }

        return score.coerceIn(0.0, 1.0)
    }

    private companion object {
        // Below this score a match is treated as unresolved rather than risking an
        // incorrectly attached filler/recap classification.
        const val MIN_CONFIDENCE = 0.72
    }
}

/** lowercase, trimmed, punctuation and repeated-whitespace normalized. */
internal fun normalizeTitle(raw: String): String {
    val lower = raw.lowercase().trim()
    val withoutPunctuation = lower.replace(Regex("[\\p{Punct}]"), " ")
    return withoutPunctuation.replace(Regex("\\s+"), " ").trim()
}

/** 0.0 (unrelated) .. 1.0 (identical) title similarity over normalized titles. */
internal fun titleSimilarity(a: String, b: String): Double {
    if (a.isBlank() || b.isBlank()) return 0.0
    if (a == b) return 1.0
    if (a.contains(b) || b.contains(a)) {
        val shorter = min(a.length, b.length).toDouble()
        val longer = max(a.length, b.length).toDouble()
        val ratio = shorter / longer
        // A pure substring match scales with how much of the longer title is covered.
        // Near-equal lengths (ratio close to 1, e.g. a trailing punctuation difference)
        // score close to 1.0; a short title fully contained in a much longer one (e.g.
        // "naruto" inside "naruto shippuden") scores low so sequels are not confused
        // with their parent series.
        return (0.3 + 0.65 * ratio).coerceIn(0.0, 0.97)
    }
    val distance = levenshtein(a, b)
    val maxLen = max(a.length, b.length)
    if (maxLen == 0) return 1.0
    return (1.0 - distance.toDouble() / maxLen).coerceIn(0.0, 1.0)
}

private fun levenshtein(a: String, b: String): Int {
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) {
                dp[i - 1][j - 1]
            } else {
                1 + min(dp[i - 1][j], min(dp[i][j - 1], dp[i - 1][j - 1]))
            }
        }
    }
    return dp[a.length][b.length]
}
