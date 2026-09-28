package com.nuvio.app.features.anime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TenraiPagination(
    @SerialName("last_visible_page") val lastVisiblePage: Int = 1,
    @SerialName("has_next_page") val hasNextPage: Boolean = false,
)

// Tenrai's episode `mal_id` is the anime-local episode number (1, 2, 3, ...), not a
// MyAnimeList anime id.
@Serializable
internal data class TenraiEpisodeDto(
    @SerialName("mal_id") val episodeNumber: Int,
    val title: String? = null,
    @SerialName("title_japanese") val titleJapanese: String? = null,
    @SerialName("title_romanji") val titleRomanji: String? = null,
    val aired: String? = null,
    val filler: Boolean = false,
    val recap: Boolean = false,
)

@Serializable
internal data class TenraiEpisodesResponse(
    val pagination: TenraiPagination = TenraiPagination(),
    val data: List<TenraiEpisodeDto> = emptyList(),
)

@Serializable
internal data class TenraiTitleEntryDto(
    val type: String? = null,
    val title: String? = null,
)

@Serializable
internal data class TenraiAiredDateDto(
    val day: Int? = null,
    val month: Int? = null,
    val year: Int? = null,
)

@Serializable
internal data class TenraiAiredPropDto(
    val from: TenraiAiredDateDto? = null,
)

@Serializable
internal data class TenraiAiredDto(
    val prop: TenraiAiredPropDto? = null,
)

@Serializable
internal data class TenraiSearchAnimeDto(
    @SerialName("mal_id") val malId: Int,
    val title: String? = null,
    @SerialName("title_english") val titleEnglish: String? = null,
    @SerialName("title_japanese") val titleJapanese: String? = null,
    @SerialName("title_synonyms") val titleSynonyms: List<String> = emptyList(),
    val titles: List<TenraiTitleEntryDto> = emptyList(),
    val type: String? = null,
    val episodes: Int? = null,
    val aired: TenraiAiredDto? = null,
)

@Serializable
internal data class TenraiSearchResponse(
    val pagination: TenraiPagination = TenraiPagination(),
    val data: List<TenraiSearchAnimeDto> = emptyList(),
)
