package com.nuvio.app.features.anime

import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaVideo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnimeEpisodeMatchingTest {

    private fun video(season: Int?, episode: Int?) = MetaVideo(
        id = "s${season}e$episode",
        title = "Episode $episode",
        season = season,
        episode = episode,
    )

    @Test
    fun singleSeasonShowClassificationsPassThrough() {
        val meta = MetaDetails(
            id = "tmdb:1",
            type = "series",
            name = "Naruto",
            videos = (1..5).map { video(1, it) },
        )
        val raw = mapOf(3 to AnimeEpisodeType.FILLER)
        val safe = meta.safeAnimeEpisodeClassifications(raw)
        assertEquals(raw, safe)

        val bySeasonEpisode = meta.animeEpisodeClassificationsBySeasonEpisode(safe)
        assertEquals(AnimeEpisodeType.FILLER, bySeasonEpisode[1 to 3])
    }

    @Test
    fun multiSeasonShowDropsClassificationsToAvoidMislabeling() {
        val meta = MetaDetails(
            id = "tmdb:2",
            type = "series",
            name = "Some Show",
            videos = (1..3).map { video(1, it) } + (1..3).map { video(2, it) },
        )
        val raw = mapOf(2 to AnimeEpisodeType.FILLER)
        val safe = meta.safeAnimeEpisodeClassifications(raw)
        assertTrue(safe.isEmpty(), "multi-season classification mapping must be dropped as unsafe")
        assertTrue(meta.animeEpisodeClassificationsBySeasonEpisode(safe).isEmpty())
    }

    @Test
    fun specialsSeasonZeroDoesNotCountAsASecondRealSeason() {
        val meta = MetaDetails(
            id = "tmdb:3",
            type = "series",
            name = "Naruto",
            videos = (1..5).map { video(1, it) } + listOf(video(0, 1)),
        )
        val raw = mapOf(3 to AnimeEpisodeType.RECAP)
        val safe = meta.safeAnimeEpisodeClassifications(raw)
        assertEquals(raw, safe)
    }
}
