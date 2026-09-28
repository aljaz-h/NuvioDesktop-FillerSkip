package com.nuvio.app.features.anime

import com.nuvio.app.features.details.MetaDetails

/**
 * Tenrai/MAL episode numbering only reliably lines up with Nuvio's (season, episode) model
 * when the show is represented as a single season (the common case for long-running shonen
 * like Naruto, where Nuvio episode N == MAL episode N). When more than one real season is
 * present, season boundaries and MAL-entry numbering can diverge in ways that would silently
 * mislabel episodes, so classifications are intentionally dropped (UNKNOWN/no badge) rather
 * than risk an incorrect FILLER/RECAP label. See README "Known limitations".
 */
fun MetaDetails.safeAnimeEpisodeClassifications(
    byEpisodeNumber: Map<Int, AnimeEpisodeType>,
): Map<Int, AnimeEpisodeType> {
    if (byEpisodeNumber.isEmpty()) return emptyMap()
    val realSeasons = videos.mapNotNull { it.season }.filter { it > 0 }.toSet()
    return if (realSeasons.size > 1) emptyMap() else byEpisodeNumber
}

/** UI-facing form keyed like other per-episode maps in the details screen (season, episode). */
fun MetaDetails.animeEpisodeClassificationsBySeasonEpisode(
    safeByEpisodeNumber: Map<Int, AnimeEpisodeType>,
): Map<Pair<Int, Int>, AnimeEpisodeType> {
    if (safeByEpisodeNumber.isEmpty()) return emptyMap()
    val season = videos.mapNotNull { it.season }.filter { it > 0 }.toSet().singleOrNull() ?: 1
    return safeByEpisodeNumber.mapKeys { (episode, _) -> season to episode }
}
