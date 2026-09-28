package com.nuvio.app.features.anime

import com.nuvio.app.features.details.MetaDetails

/**
 * Conservative anime heuristic: requires BOTH the TMDB "Animation" genre AND a Japanese
 * origin signal (original_language == "ja", or origin country JP). Genre alone is not
 * enough - Western/Korean/other animated shows must not be sent to Tenrai. This keeps
 * Tenrai traffic limited to titles that are actually likely to exist on MyAnimeList.
 */
object AnimeDetector {
    private val ANIMATION_GENRES = setOf("animation", "anime")
    private val JAPANESE_LANGUAGE_CODES = setOf("ja")
    private val JAPANESE_COUNTRY_CODES = setOf("jp")

    fun isLikelyAnime(meta: MetaDetails): Boolean {
        if (meta.type != "series" && meta.type != "movie") return false

        val hasAnimationGenre = meta.genres.any { genre ->
            genre.trim().lowercase() in ANIMATION_GENRES
        }
        if (!hasAnimationGenre) return false

        val isJapaneseLanguage = meta.language?.trim()?.lowercase() in JAPANESE_LANGUAGE_CODES
        val isJapaneseCountry = meta.country?.trim()?.lowercase() in JAPANESE_COUNTRY_CODES
        return isJapaneseLanguage || isJapaneseCountry
    }
}
