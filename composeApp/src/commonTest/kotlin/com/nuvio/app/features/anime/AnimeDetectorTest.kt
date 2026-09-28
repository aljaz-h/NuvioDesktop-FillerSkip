package com.nuvio.app.features.anime

import com.nuvio.app.features.details.MetaDetails
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimeDetectorTest {

    private fun meta(
        type: String = "series",
        genres: List<String> = emptyList(),
        language: String? = null,
        country: String? = null,
    ) = MetaDetails(id = "tmdb:1", type = type, name = "Test", genres = genres, language = language, country = country)

    @Test
    fun detectsJapaneseAnimationAsAnime() {
        assertTrue(AnimeDetector.isLikelyAnime(meta(genres = listOf("Animation"), language = "ja")))
    }

    @Test
    fun doesNotFlagWesternAnimationAsAnime() {
        assertFalse(AnimeDetector.isLikelyAnime(meta(genres = listOf("Animation"), language = "en")))
    }

    @Test
    fun doesNotFlagNonAnimatedJapaneseShowAsAnime() {
        assertFalse(AnimeDetector.isLikelyAnime(meta(genres = listOf("Drama"), language = "ja")))
    }

    @Test
    fun countryFallbackAlsoQualifies() {
        assertTrue(AnimeDetector.isLikelyAnime(meta(genres = listOf("Animation"), country = "JP")))
    }

    @Test
    fun moviesCanQualifyToo() {
        assertTrue(AnimeDetector.isLikelyAnime(meta(type = "movie", genres = listOf("Animation"), language = "ja")))
    }
}
