package com.nuvio.app.features.anime

import co.touchlab.kermit.Logger
import com.nuvio.app.features.details.MetaDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

private val log = Logger.withTag("Anime")

/**
 * Orchestrates anime detection, MAL id resolution and Tenrai episode classification with
 * on-disk + in-memory caching and request de-duplication. This is the only entry point the
 * rest of the app should use - UI and player code must never call [TenraiEpisodeProvider] or
 * [TenraiAnimeIdResolver] directly.
 *
 * Failure behavior: any Tenrai/network failure at any stage simply yields an empty
 * classification map (or falls back to a stale-but-present cache). Nothing here ever
 * throws into a caller, and nothing here can block or break playback.
 */
object AnimeEpisodeClassificationRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val resolver: AnimeIdResolver = TenraiAnimeIdResolver()
    private val provider: AnimeEpisodeProvider = TenraiEpisodeProvider()

    private val mutex = Mutex()

    // In-memory hot caches mirroring the on-disk cache, keyed by resolutionKey() / malId.
    private val resolutionCache = HashMap<String, Int?>()
    private val episodeCache = HashMap<Int, Map<Int, AnimeEpisodeType>>()
    private val inFlight = HashMap<String, CompletableDeferred<Map<Int, AnimeEpisodeType>>>()

    private const val RESOLUTION_TTL_MS = 14L * 24 * 60 * 60 * 1000L // 14 days
    private const val EPISODES_TTL_MS = 30L * 24 * 60 * 60 * 1000L // 30 days

    /**
     * Returns whatever classification data is already known in-memory, without ever
     * triggering network I/O. Safe to call from hot paths (e.g. next-episode resolution)
     * that must not be blocked by a Tenrai request.
     */
    fun peekClassifications(meta: MetaDetails): Map<Int, AnimeEpisodeType> {
        val malId = resolutionCache[resolutionKey(meta)] ?: return emptyMap()
        return episodeCache[malId] ?: emptyMap()
    }

    /**
     * Resolves (detector -> resolver -> provider, all cached) and returns the episode
     * classifications for [meta]. Intended to be called from a details-screen load effect;
     * concurrent calls for the same title are de-duplicated onto a single in-flight request.
     */
    suspend fun getClassifications(meta: MetaDetails): Map<Int, AnimeEpisodeType> {
        if (!AnimeDetector.isLikelyAnime(meta)) return emptyMap()
        log.d { "[Anime] candidate detected: '${meta.name}'" }

        val key = resolutionKey(meta)
        val awaited = mutex.withLock { inFlight[key] }
        if (awaited != null) return awaited.await()

        val deferred = CompletableDeferred<Map<Int, AnimeEpisodeType>>()
        val owns = mutex.withLock {
            if (inFlight.containsKey(key)) {
                false
            } else {
                inFlight[key] = deferred
                true
            }
        }
        if (!owns) {
            return mutex.withLock { inFlight[key] }?.await() ?: emptyMap()
        }

        return try {
            val malId = resolveMalId(meta, key)
            val result = if (malId != null) fetchEpisodes(malId) else emptyMap()
            deferred.complete(result)
            result
        } catch (cancelled: CancellationException) {
            deferred.completeExceptionally(cancelled)
            throw cancelled
        } catch (e: Exception) {
            log.w { "[Anime] classification pipeline failed for '${meta.name}': ${e.message}" }
            deferred.complete(emptyMap())
            emptyMap()
        } finally {
            mutex.withLock { inFlight.remove(key) }
        }
    }

    fun clearCache() {
        resolutionCache.clear()
        episodeCache.clear()
    }

    private suspend fun resolveMalId(meta: MetaDetails, key: String): Int? {
        if (resolutionCache.containsKey(key)) return resolutionCache[key]

        tenraiDebugMalIdOverride()?.let { override ->
            log.d { "[Anime] using NUVIO_TENRAI_DEBUG_MAL_ID override=$override for '${meta.name}'" }
            resolutionCache[key] = override
            return override
        }

        AnimeEpisodeCacheStorage.loadResolution(key)?.let { cachedJson ->
            val cached = runCatching { json.decodeFromString(CachedResolution.serializer(), cachedJson) }.getOrNull()
            if (cached != null && !isExpired(cached.cachedAtMs, RESOLUTION_TTL_MS)) {
                log.d { "[Anime] using cached MAL id=${cached.malId} for '${meta.name}'" }
                resolutionCache[key] = cached.malId
                return cached.malId
            }
        }

        val query = buildResolveQuery(meta)
        if (query == null) {
            resolutionCache[key] = null
            return null
        }

        log.d { "[Anime] resolving MAL ID for '${meta.name}'" }
        val resolution = try {
            resolver.resolve(query)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            log.w { "[Anime] MAL id resolution failed for '${meta.name}': ${e.message}" }
            null
        }

        resolutionCache[key] = resolution?.malId
        if (resolution != null) {
            val payload = CachedResolution(
                malId = resolution.malId,
                confidence = resolution.confidence,
                matchedTitle = resolution.matchedTitle,
                cachedAtMs = animeCurrentTimeMs(),
            )
            AnimeEpisodeCacheStorage.saveResolution(key, json.encodeToString(CachedResolution.serializer(), payload))
        }
        return resolution?.malId
    }

    private suspend fun fetchEpisodes(malId: Int): Map<Int, AnimeEpisodeType> {
        episodeCache[malId]?.let { return it }

        AnimeEpisodeCacheStorage.loadEpisodes(malId)?.let { cachedJson ->
            val cached = runCatching { json.decodeFromString(CachedEpisodes.serializer(), cachedJson) }.getOrNull()
            if (cached != null && !isExpired(cached.cachedAtMs, EPISODES_TTL_MS)) {
                log.d { "[Anime] using cached episode metadata for malId=$malId" }
                val map = cached.episodes.associate { it.episodeNumber to parseType(it.type) }
                episodeCache[malId] = map
                return map
            }
        }

        log.d { "[Anime] loading Tenrai episodes for malId=$malId" }
        val episodes = try {
            provider.getEpisodes(malId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            log.w { "[Anime] Tenrai episode load failed for malId=$malId: ${e.message}" }
            emptyList()
        }
        if (episodes.isEmpty()) return emptyMap()

        episodes.forEach { episode ->
            log.d { "[Anime] episode ${episode.episodeNumber} classified as ${episode.type}" }
        }

        val map = episodes.associate { it.episodeNumber to it.type }
        episodeCache[malId] = map
        val payload = CachedEpisodes(
            episodes = episodes.map { CachedEpisodeEntry(it.episodeNumber, it.type.name, it.title, it.airedAt) },
            cachedAtMs = animeCurrentTimeMs(),
        )
        AnimeEpisodeCacheStorage.saveEpisodes(malId, json.encodeToString(CachedEpisodes.serializer(), payload))
        return map
    }

    private fun buildResolveQuery(meta: MetaDetails): AnimeResolveQuery? {
        val title = meta.name.trim().takeIf { it.isNotBlank() } ?: return null
        val year = meta.releaseInfo?.take(4)?.toIntOrNull()
        // Only meaningful for the common single-"season" MAL mapping (see episode matching
        // limitations); used purely as a soft disambiguation signal, never authoritative.
        val episodeCount = meta.videos
            .count { it.season == null || it.season == 1 }
            .takeIf { it > 0 }
        return AnimeResolveQuery(
            title = title,
            mediaType = meta.type,
            year = year,
            episodeCount = episodeCount,
        )
    }

    private fun resolutionKey(meta: MetaDetails): String = "${meta.type}:${meta.id}"

    private fun parseType(raw: String): AnimeEpisodeType =
        runCatching { AnimeEpisodeType.valueOf(raw) }.getOrDefault(AnimeEpisodeType.UNKNOWN)

    private fun isExpired(cachedAtMs: Long, ttlMs: Long): Boolean =
        animeCurrentTimeMs() - cachedAtMs > ttlMs
}
