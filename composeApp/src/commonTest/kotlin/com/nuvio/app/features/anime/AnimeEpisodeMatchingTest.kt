package com.nuvio.app.features.anime

import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaVideo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnimeEpisodeMatchingTest {

    private fun video(season: Int?, episode: Int?, released: String? = null) = MetaVideo(
        id = "s${season}e$episode",
        title = "Episode $episode",
        season = season,
        episode = episode,
        released = released,
    )

    private fun malEpisode(number: Int, type: AnimeEpisodeType, airedAt: String?) = AnimeEpisodeMetadata(
        episodeNumber = number,
        type = type,
        title = "MAL Episode $number",
        airedAt = airedAt,
    )

    @Test
    fun singleSeasonShowMatchesByAiredDate() {
        val meta = MetaDetails(
            id = "tmdb:1",
            type = "series",
            name = "Naruto",
            videos = (1..5).map { video(1, it, released = "2002-10-0${it}") },
        )
        val malEpisodes = (1..5).map {
            malEpisode(it, if (it == 3) AnimeEpisodeType.FILLER else AnimeEpisodeType.NORMAL, "2002-10-0$it" + "T00:00:00+00:00")
        }
        val result = meta.matchAnimeEpisodeClassifications(malEpisodes)
        assertEquals(AnimeEpisodeType.FILLER, result[1 to 3])
        assertEquals(AnimeEpisodeType.NORMAL, result[1 to 1])
    }

    @Test
    fun pseudoSeasonSplitShowMatchesByAiredDateAcrossSeasonBoundaries() {
        // Nuvio splits one continuous MAL entry into two "seasons" of 3 episodes each, with
        // per-season episode numbers restarting at 1 - exactly like arc-based catalog
        // pagination for long-running shonen (e.g. Bleach).
        val meta = MetaDetails(
            id = "tmdb:2",
            type = "series",
            name = "Bleach",
            videos = listOf(
                video(1, 1, "2004-10-05"),
                video(1, 2, "2004-10-12"),
                video(1, 3, "2004-10-19"),
                video(2, 1, "2004-10-26"),
                video(2, 2, "2004-11-02"),
                video(2, 3, "2004-11-09"),
            ),
        )
        val malEpisodes = listOf(
            malEpisode(1, AnimeEpisodeType.NORMAL, "2004-10-05T00:00:00+00:00"),
            malEpisode(2, AnimeEpisodeType.NORMAL, "2004-10-12T00:00:00+00:00"),
            malEpisode(3, AnimeEpisodeType.NORMAL, "2004-10-19T00:00:00+00:00"),
            malEpisode(4, AnimeEpisodeType.FILLER, "2004-10-26T00:00:00+00:00"),
            malEpisode(5, AnimeEpisodeType.FILLER, "2004-11-02T00:00:00+00:00"),
            malEpisode(6, AnimeEpisodeType.NORMAL, "2004-11-09T00:00:00+00:00"),
        )
        val result = meta.matchAnimeEpisodeClassifications(malEpisodes)
        // Pseudo-season 2 episode 1-2 (global MAL episodes 4-5) are filler.
        assertEquals(AnimeEpisodeType.FILLER, result[2 to 1])
        assertEquals(AnimeEpisodeType.FILLER, result[2 to 2])
        assertEquals(AnimeEpisodeType.NORMAL, result[2 to 3])
        assertEquals(AnimeEpisodeType.NORMAL, result[1 to 1])
    }

    @Test
    fun genuinelyDifferentSequelSeasonIsNotMisclassified() {
        // A real new season (its own MAL entry, airing years later) must not be matched
        // against the original run's MAL episode list just because Nuvio also numbers it
        // "season 2" - its dates simply won't appear in that list, so it stays unclassified.
        val meta = MetaDetails(
            id = "tmdb:3",
            type = "series",
            name = "Bleach",
            videos = listOf(
                video(1, 1, "2004-10-05"),
                video(1, 2, "2004-10-12"),
                video(2, 1, "2022-10-11"),
                video(2, 2, "2022-10-18"),
            ),
        )
        val malEpisodes = listOf(
            malEpisode(1, AnimeEpisodeType.NORMAL, "2004-10-05T00:00:00+00:00"),
            malEpisode(2, AnimeEpisodeType.NORMAL, "2004-10-12T00:00:00+00:00"),
        )
        val result = meta.matchAnimeEpisodeClassifications(malEpisodes)
        assertEquals(AnimeEpisodeType.NORMAL, result[1 to 1])
        assertTrue((2 to 1) !in result, "sequel season episode must stay unclassified, not guessed")
        assertTrue((2 to 2) !in result)
    }

    @Test
    fun fallsBackToNumberMatchingWhenNoAiredDatesAvailable() {
        val meta = MetaDetails(
            id = "tmdb:4",
            type = "series",
            name = "Cowboy Bebop",
            videos = (1..3).map { video(1, it, released = null) },
        )
        val malEpisodes = (1..3).map {
            malEpisode(it, if (it == 2) AnimeEpisodeType.RECAP else AnimeEpisodeType.NORMAL, airedAt = null)
        }
        val result = meta.matchAnimeEpisodeClassifications(malEpisodes)
        assertEquals(AnimeEpisodeType.RECAP, result[1 to 2])
    }

    @Test
    fun numberFallbackDropsAmbiguousMultiSeasonShowsWithNoDates() {
        val meta = MetaDetails(
            id = "tmdb:5",
            type = "series",
            name = "Some Show",
            videos = (1..2).map { video(1, it) } + (1..2).map { video(2, it) },
        )
        val malEpisodes = (1..4).map { malEpisode(it, AnimeEpisodeType.FILLER, airedAt = null) }
        val result = meta.matchAnimeEpisodeClassifications(malEpisodes)
        assertTrue(result.isEmpty(), "ambiguous multi-season mapping without dates must be dropped, not guessed")
    }
}
