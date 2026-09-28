package com.nuvio.app.features.anime

/**
 * Tenrai reports independent `filler`/`recap` booleans. If both are ever true for the same
 * episode, RECAP wins (an episode that recaps prior events is still not new "filler" content
 * in the sense users care about skipping first).
 */
enum class AnimeEpisodeType {
    NORMAL,
    FILLER,
    RECAP,
    UNKNOWN,
}

data class AnimeEpisodeMetadata(
    val episodeNumber: Int,
    val type: AnimeEpisodeType,
    val title: String?,
    val airedAt: String?,
)

data class AnimeResolution(
    val malId: Int,
    val confidence: Double,
    val matchedTitle: String,
)

data class AnimeResolveQuery(
    val title: String,
    val originalTitle: String? = null,
    val alternativeTitles: List<String> = emptyList(),
    val year: Int? = null,
    val mediaType: String,
    val episodeCount: Int? = null,
)
