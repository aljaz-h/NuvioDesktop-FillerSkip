package com.nuvio.app.features.anime

import co.touchlab.kermit.Logger
import com.nuvio.app.features.addons.httpGetText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

interface AnimeEpisodeProvider {
    suspend fun getEpisodes(malId: Int): List<AnimeEpisodeMetadata>
}

private val log = Logger.withTag("Anime")

/**
 * [fetchPageText] defaults to a real Tenrai request but can be swapped out in tests for a
 * fake page-by-page fetcher, so pagination and JSON-mapping behavior can be verified without
 * a live network/mocking framework.
 */
class TenraiEpisodeProvider(
    private val fetchPageText: suspend (malId: Int, page: Int) -> String = { malId, page ->
        httpGetText("${TenraiConfig.BASE_URL}/anime/$malId/episodes?page=$page")
    },
) : AnimeEpisodeProvider {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun getEpisodes(malId: Int): List<AnimeEpisodeMetadata> {
        val episodes = mutableListOf<AnimeEpisodeMetadata>()
        var page = 1
        try {
            while (page <= MAX_PAGES) {
                val text = fetchPageText(malId, page)
                val response = json.decodeFromString(TenraiEpisodesResponse.serializer(), text)
                episodes += response.data.map { it.toDomain() }
                if (!response.pagination.hasNextPage) break
                page += 1
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            // Partial pagination failure: keep whatever pages we already fetched rather
            // than discarding them. Tenrai being flaky must never break playback.
            log.w { "[Anime] Tenrai request failed for malId=$malId page=$page: ${e.message}" }
        }
        return episodes
    }

    private companion object {
        // Safety cap against malformed/looping pagination data (~1000+ episodes).
        const val MAX_PAGES = 50
    }
}

internal fun TenraiEpisodeDto.toDomain(): AnimeEpisodeMetadata {
    // If Tenrai ever reports both flags true for one episode, RECAP wins (documented in
    // AnimeEpisodeType).
    val type = when {
        recap -> AnimeEpisodeType.RECAP
        filler -> AnimeEpisodeType.FILLER
        else -> AnimeEpisodeType.NORMAL
    }
    return AnimeEpisodeMetadata(
        episodeNumber = episodeNumber,
        type = type,
        title = title?.takeIf { it.isNotBlank() },
        airedAt = aired,
    )
}
