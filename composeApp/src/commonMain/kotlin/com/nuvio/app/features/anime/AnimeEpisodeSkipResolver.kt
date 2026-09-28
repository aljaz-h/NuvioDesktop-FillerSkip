package com.nuvio.app.features.anime

import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.player.skip.PlayerNextEpisodeRules
import com.nuvio.app.features.settings.EpisodeHandlingMode

/**
 * Filler/recap-aware wrapper around [PlayerNextEpisodeRules.resolveNextEpisode]. Walks
 * forward past consecutive episodes that should be auto-skipped (per [fillerHandling]/
 * [recapHandling]) until it finds one that should not be, or runs out of episodes.
 *
 * Unknown classification is NEVER skipped - only a confident FILLER/RECAP classification
 * combined with a SKIP/HIDE setting causes an episode to be walked past.
 */
object AnimeEpisodeSkipResolver {

    data class Result(
        val nextEpisode: MetaVideo?,
        val skippedEpisodes: List<MetaVideo>,
        val skippedFillerCount: Int,
        val skippedRecapCount: Int,
    )

    fun findNextPlayableEpisode(
        currentSeason: Int,
        currentEpisode: Int,
        videos: List<MetaVideo>,
        classifications: Map<Int, AnimeEpisodeType>,
        fillerHandling: EpisodeHandlingMode,
        recapHandling: EpisodeHandlingMode,
    ): Result {
        if (classifications.isEmpty() ||
            (fillerHandling == EpisodeHandlingMode.SHOW && recapHandling == EpisodeHandlingMode.SHOW)
        ) {
            val next = PlayerNextEpisodeRules.resolveNextEpisode(videos, currentSeason, currentEpisode)
            return Result(next, emptyList(), 0, 0)
        }

        var season = currentSeason
        var episode = currentEpisode
        val skipped = mutableListOf<MetaVideo>()
        var fillerCount = 0
        var recapCount = 0

        repeat(MAX_SKIPS) {
            val candidate = PlayerNextEpisodeRules.resolveNextEpisode(videos, season, episode)
                ?: return Result(null, skipped, fillerCount, recapCount)

            val type = candidate.episode?.let { classifications[it] } ?: AnimeEpisodeType.UNKNOWN
            val shouldSkip = when (type) {
                AnimeEpisodeType.FILLER -> fillerHandling != EpisodeHandlingMode.SHOW
                AnimeEpisodeType.RECAP -> recapHandling != EpisodeHandlingMode.SHOW
                AnimeEpisodeType.NORMAL, AnimeEpisodeType.UNKNOWN -> false
            }
            if (!shouldSkip) return Result(candidate, skipped, fillerCount, recapCount)

            skipped += candidate
            when (type) {
                AnimeEpisodeType.FILLER -> fillerCount++
                AnimeEpisodeType.RECAP -> recapCount++
                else -> Unit
            }

            val nextSeason = candidate.season ?: return Result(null, skipped, fillerCount, recapCount)
            val nextEpisodeNumber = candidate.episode ?: return Result(null, skipped, fillerCount, recapCount)
            season = nextSeason
            episode = nextEpisodeNumber
        }
        return Result(null, skipped, fillerCount, recapCount)
    }

    private const val MAX_SKIPS = 2000
}
