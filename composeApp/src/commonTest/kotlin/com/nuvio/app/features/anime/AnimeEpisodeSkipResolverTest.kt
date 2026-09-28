package com.nuvio.app.features.anime

import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.settings.EpisodeHandlingMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnimeEpisodeSkipResolverTest {

    private fun episode(number: Int) = MetaVideo(
        id = "ep-$number",
        title = "Episode $number",
        season = 1,
        episode = number,
    )

    private val videos = (1..20).map(::episode)

    private fun classifications(vararg entries: Pair<Int, AnimeEpisodeType>): Map<Pair<Int, Int>, AnimeEpisodeType> =
        entries.associate { (episodeNumber, type) -> (1 to episodeNumber) to type }

    @Test
    fun showModeNeverSkips() {
        val result = AnimeEpisodeSkipResolver.findNextPlayableEpisode(
            currentSeason = 1,
            currentEpisode = 10,
            videos = videos,
            classifications = classifications(11 to AnimeEpisodeType.FILLER, 12 to AnimeEpisodeType.FILLER, 13 to AnimeEpisodeType.NORMAL),
            fillerHandling = EpisodeHandlingMode.SHOW,
            recapHandling = EpisodeHandlingMode.SHOW,
        )
        assertEquals(11, result.nextEpisode?.episode)
        assertEquals(0, result.skippedFillerCount)
    }

    @Test
    fun skipModeWalksPastConsecutiveFillerEpisodes() {
        val result = AnimeEpisodeSkipResolver.findNextPlayableEpisode(
            currentSeason = 1,
            currentEpisode = 10,
            videos = videos,
            classifications = classifications(11 to AnimeEpisodeType.FILLER, 12 to AnimeEpisodeType.FILLER, 13 to AnimeEpisodeType.NORMAL),
            fillerHandling = EpisodeHandlingMode.SKIP,
            recapHandling = EpisodeHandlingMode.SHOW,
        )
        assertEquals(13, result.nextEpisode?.episode)
        assertEquals(2, result.skippedFillerCount)
        assertEquals(listOf(11, 12), result.skippedEpisodes.map { it.episode })
    }

    @Test
    fun hideModeAlsoWalksPastFillerForAutomaticProgression() {
        val result = AnimeEpisodeSkipResolver.findNextPlayableEpisode(
            currentSeason = 1,
            currentEpisode = 10,
            videos = videos,
            classifications = classifications(11 to AnimeEpisodeType.FILLER, 12 to AnimeEpisodeType.NORMAL),
            fillerHandling = EpisodeHandlingMode.HIDE,
            recapHandling = EpisodeHandlingMode.SHOW,
        )
        assertEquals(12, result.nextEpisode?.episode)
        assertEquals(1, result.skippedFillerCount)
    }

    @Test
    fun fillerAndRecapSettingsAreIndependent() {
        // 11 recap, 12 filler, 13 normal; filler=SKIP, recap=SHOW -> next is 11 (recap shown).
        val result = AnimeEpisodeSkipResolver.findNextPlayableEpisode(
            currentSeason = 1,
            currentEpisode = 10,
            videos = videos,
            classifications = classifications(11 to AnimeEpisodeType.RECAP, 12 to AnimeEpisodeType.FILLER, 13 to AnimeEpisodeType.NORMAL),
            fillerHandling = EpisodeHandlingMode.SKIP,
            recapHandling = EpisodeHandlingMode.SHOW,
        )
        assertEquals(11, result.nextEpisode?.episode)
        assertEquals(0, result.skippedFillerCount)
        assertEquals(0, result.skippedRecapCount)
    }

    @Test
    fun recapSkipIsIndependentFromFillerSkip() {
        val result = AnimeEpisodeSkipResolver.findNextPlayableEpisode(
            currentSeason = 1,
            currentEpisode = 10,
            videos = videos,
            classifications = classifications(11 to AnimeEpisodeType.RECAP, 12 to AnimeEpisodeType.NORMAL),
            fillerHandling = EpisodeHandlingMode.SHOW,
            recapHandling = EpisodeHandlingMode.SKIP,
        )
        assertEquals(12, result.nextEpisode?.episode)
        assertEquals(1, result.skippedRecapCount)
        assertEquals(0, result.skippedFillerCount)
    }

    @Test
    fun unknownClassificationIsNeverAutomaticallySkipped() {
        // No classification data at all for episode 11 (e.g. resolution failed).
        val result = AnimeEpisodeSkipResolver.findNextPlayableEpisode(
            currentSeason = 1,
            currentEpisode = 10,
            videos = videos,
            classifications = emptyMap(),
            fillerHandling = EpisodeHandlingMode.SKIP,
            recapHandling = EpisodeHandlingMode.SKIP,
        )
        assertEquals(11, result.nextEpisode?.episode)
        assertEquals(0, result.skippedFillerCount)
        assertEquals(0, result.skippedRecapCount)
    }

    @Test
    fun returnsNullWhenTrailingEpisodesAreAllSkipped() {
        val result = AnimeEpisodeSkipResolver.findNextPlayableEpisode(
            currentSeason = 1,
            currentEpisode = 18,
            videos = videos,
            classifications = classifications(19 to AnimeEpisodeType.FILLER, 20 to AnimeEpisodeType.FILLER),
            fillerHandling = EpisodeHandlingMode.SKIP,
            recapHandling = EpisodeHandlingMode.SHOW,
        )
        assertNull(result.nextEpisode)
        assertEquals(2, result.skippedFillerCount)
    }

    @Test
    fun matchesByExactSeasonAndEpisodeAcrossPseudoSeasonBoundaries() {
        // Crossing from season 1 into season 2: season 2 episode 1 is classified as filler,
        // and must be matched by (season, episode) - not confused with season 1's own
        // episode 1 (same episode number, different season, different classification).
        val multiSeasonVideos = (1..5).map { episode(it) } +
            (1..5).map { MetaVideo(id = "s2ep-$it", title = "S2E$it", season = 2, episode = it) }
        val result = AnimeEpisodeSkipResolver.findNextPlayableEpisode(
            currentSeason = 1,
            currentEpisode = 5,
            videos = multiSeasonVideos,
            classifications = mapOf(
                (1 to 1) to AnimeEpisodeType.NORMAL,
                (2 to 1) to AnimeEpisodeType.FILLER,
            ),
            fillerHandling = EpisodeHandlingMode.SKIP,
            recapHandling = EpisodeHandlingMode.SHOW,
        )
        assertEquals(2, result.nextEpisode?.season)
        assertEquals(2, result.nextEpisode?.episode)
        assertEquals(1, result.skippedFillerCount)
    }
}
