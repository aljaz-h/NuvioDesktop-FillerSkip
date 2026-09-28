package com.nuvio.app.features.anime

import com.nuvio.app.features.details.MetaDetails

/**
 * Lines up Tenrai/MAL episode classifications with Nuvio's own (season, episode) episode
 * identity.
 *
 * Nuvio's season/episode numbers do NOT reliably correspond to MAL's flat numbering: many
 * catalogs split one long-running MAL entry into several arc-based "seasons" for display,
 * and per-season episode numbers usually restart at 1 - so "season 4 episode 1" can really
 * be the show's global episode 64. Naively assuming a 1:1 number match (as a single-season
 * safety check alone would) either drops classifications for every such show, or - if that
 * check were relaxed - risks silently mislabeling episodes.
 *
 * Primary strategy: match by AIRED DATE. Both Tenrai (`aired`) and Nuvio (`released`) carry
 * an absolute air date per episode, which does not depend on how either side numbers
 * seasons. This works regardless of how many pseudo-seasons a show is split into, and it
 * naturally excludes a genuinely different season (e.g. a sequel/continuation with its own
 * MAL entry, airing years later) from being matched against the wrong MAL id's episode list,
 * since its dates simply won't appear in that list.
 *
 * Fallback: when dates are unusable on either side (rare), classification is only applied
 * when the show is a single real season, so Nuvio episode N can be assumed to equal MAL
 * episode N. If that doesn't hold either, no classification is applied - UNKNOWN/no badge
 * beats a silently wrong one.
 */
fun MetaDetails.matchAnimeEpisodeClassifications(
    episodes: List<AnimeEpisodeMetadata>,
): Map<Pair<Int, Int>, AnimeEpisodeType> {
    if (episodes.isEmpty()) return emptyMap()

    val videosWithIdentity = videos.filter { it.season != null && it.episode != null }
    if (videosWithIdentity.isEmpty()) return emptyMap()

    val typeByAiredDate = HashMap<String, AnimeEpisodeType>()
    for (episode in episodes) {
        val date = episode.airedAt?.take(10)?.takeIf { it.length == 10 } ?: continue
        typeByAiredDate[date] = episode.type
    }

    if (typeByAiredDate.isNotEmpty()) {
        val byDate = HashMap<Pair<Int, Int>, AnimeEpisodeType>()
        for (video in videosWithIdentity) {
            val date = video.released?.take(10)?.takeIf { it.length == 10 } ?: continue
            val type = typeByAiredDate[date] ?: continue
            byDate[video.season!! to video.episode!!] = type
        }
        // Only trust date-matching if it actually matched a meaningful share of episodes;
        // otherwise the date formats/values likely don't line up and we should fall through
        // to the safer number-based path instead of returning a mostly-empty result.
        if (byDate.size * 2 >= videosWithIdentity.size) return byDate
    }

    val realSeasons = videosWithIdentity.mapNotNull { it.season }.filter { it > 0 }.toSet()
    if (realSeasons.size > 1) return emptyMap()
    val season = realSeasons.singleOrNull() ?: 1
    val byEpisodeNumber = episodes.associate { it.episodeNumber to it.type }
    return byEpisodeNumber.mapKeys { (episodeNumber, _) -> season to episodeNumber }
}
