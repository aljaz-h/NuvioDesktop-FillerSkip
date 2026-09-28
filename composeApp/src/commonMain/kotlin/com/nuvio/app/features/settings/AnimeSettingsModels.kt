package com.nuvio.app.features.settings

/**
 * SHOW: display the episode normally (with its FILLER/RECAP badge); plays normally.
 * SKIP: still shown in the episode list with its badge and can be played manually, but
 *       automatic next-episode/autoplay progression walks past it.
 * HIDE: hidden from the normal episode list (not deleted, not marked watched); automatic
 *       progression also walks past it.
 */
enum class EpisodeHandlingMode {
    SHOW,
    SKIP,
    HIDE,
}

data class AnimeSettingsUiState(
    val fillerHandling: EpisodeHandlingMode = EpisodeHandlingMode.SHOW,
    val recapHandling: EpisodeHandlingMode = EpisodeHandlingMode.SHOW,
)
