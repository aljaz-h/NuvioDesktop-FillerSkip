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

    @Test
    fun showModeNeverSkips() {
        val classifications = mapOf(11 to AnimeEpisodeType.FILLER, 12 to AnimeEpisodeType.FILLER, 13 to AnimeEpisodeType.NORMAL)
        val result = AnimeEpisodeSkipResolver.findNextPlayableEpisode(
            currentSeason = 1,
            currentEpisode = 10,
            videos = videos,
            classifications = classifications,
            fillerHandling = EpisodeHandlingMode.SHOW,
            recapHandling = EpisodeHandlingMode.SHOW,
        )
        assertEquals(11, result.nextEpisode?.episode)
        assertEquals(0, result.skippedFillerCount)
    }

    @Test
    fun skipModeWalksPastConsecutiveFillerEpisodes() {
        val classifications = mapOf(11 to AnimeEpisodeType.FILLER, 12 to AnimeEpisodeType.FILLER, 13 to AnimeEpisodeType.NORMAL)
        val result = AnimeEpisodeSkipResolver.findNextPlayableEpisode(
            currentSeason = 1,
            currentEpisode = 10,
            videos = videos,
            classifications = classifications,
            fillerHandling = EpisodeHandlingMode.SKIP,
            recapHandling = EpisodeHandlingMode.SHOW,
        )
        assertEquals(13, result.nextEpisode?.episode)
        assertEquals(2, result.skippedFillerCount)
        assertEquals(listOf(11, 12), result.skippedEpisodes.map { it.episode })
    }

    @Test
    fun hideModeAlsoWalksPastFillerForAutomaticProgression() {
        val classifications = mapOf(11 to AnimeEpisodeType.FILLER, 12 to AnimeEpisodeType.NORMAL)
        val result = AnimeEpisodeSkipResolver.findNextPlayableEpisode(
            currentSeason = 1,
            currentEpisode = 10,
            videos = videos,
            classifications = classifications,
            fillerHandling = EpisodeHandlingMode.HIDE,
            recapHandling = EpisodeHandlingMode.SHOW,
        )
        assertEquals(12, result.nextEpisode?.episode)
        assertEquals(1, result.skippedFillerCount)
    }

    @Test
    fun fillerAndRecapSettingsAreIndependent() {
        // 11 recap, 12 filler, 13 normal; filler=SKIP, recap=SHOW -> next is 11 (recap shown).
        val classifications = mapOf(11 to AnimeEpisodeType.RECAP, 12 to AnimeEpisodeType.FILLER, 13 to AnimeEpisodeType.NORMAL)
        val result = AnimeEpisodeSkipResolver.findNextPlayableEpisode(
            currentSeason = 1,
            currentEpisode = 10,
            videos = videos,
            classifications = classifications,
            fillerHandling = EpisodeHandlingMode.SKIP,
            recapHandling = EpisodeHandlingMode.SHOW,
        )
        assertEquals(11, result.nextEpisode?.episode)
        assertEquals(0, result.skippedFillerCount)
        assertEquals(0, result.skippedRecapCount)
    }

    @Test
    fun recapSkipIsIndependentFromFillerSkip() {
        val classifications = mapOf(11 to AnimeEpisodeType.RECAP, 12 to AnimeEpisodeType.NORMAL)
        val result = AnimeEpisodeSkipResolver.findNextPlayableEpisode(
            currentSeason = 1,
            currentEpisode = 10,
            videos = videos,
            classifications = classifications,
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
        val classifications = (19..20).associateWith { AnimeEpisodeType.FILLER }
        val result = AnimeEpisodeSkipResolver.findNextPlayableEpisode(
            currentSeason = 1,
            currentEpisode = 18,
            videos = videos,
            classifications = classifications,
            fillerHandling = EpisodeHandlingMode.SKIP,
            recapHandling = EpisodeHandlingMode.SHOW,
        )
        assertNull(result.nextEpisode)
        assertEquals(2, result.skippedFillerCount)
    }
}
