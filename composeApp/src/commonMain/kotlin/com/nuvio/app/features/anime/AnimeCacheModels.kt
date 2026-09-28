package com.nuvio.app.features.anime

import kotlinx.serialization.Serializable

@Serializable
internal data class CachedResolution(
    val malId: Int,
    val confidence: Double,
    val matchedTitle: String,
    val cachedAtMs: Long,
)

@Serializable
internal data class CachedEpisodeEntry(
    val episodeNumber: Int,
    val type: String,
    val title: String?,
    val airedAt: String?,
)

@Serializable
internal data class CachedEpisodes(
    val episodes: List<CachedEpisodeEntry>,
    val cachedAtMs: Long,
)

internal expect object AnimeEpisodeCacheStorage {
    fun loadResolution(key: String): String?
    fun saveResolution(key: String, json: String)
    fun loadEpisodes(malId: Int): String?
    fun saveEpisodes(malId: Int, json: String)
}

internal expect fun animeCurrentTimeMs(): Long
