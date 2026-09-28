package com.nuvio.app.features.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AnimeSettingsRepository {
    private val _uiState = MutableStateFlow(AnimeSettingsUiState())
    val uiState: StateFlow<AnimeSettingsUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var fillerHandling = EpisodeHandlingMode.SHOW
    private var recapHandling = EpisodeHandlingMode.SHOW

    // Filler/recap handling is a device-wide preference (not profile-scoped), matching
    // other global playback defaults.
    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun setFillerHandling(mode: EpisodeHandlingMode) {
        ensureLoaded()
        if (fillerHandling == mode) return
        fillerHandling = mode
        publish()
        AnimeSettingsStorage.saveFillerHandling(mode.name)
    }

    fun setRecapHandling(mode: EpisodeHandlingMode) {
        ensureLoaded()
        if (recapHandling == mode) return
        recapHandling = mode
        publish()
        AnimeSettingsStorage.saveRecapHandling(mode.name)
    }

    private fun loadFromDisk() {
        hasLoaded = true
        fillerHandling = AnimeSettingsStorage.loadFillerHandling()
            ?.let { runCatching { EpisodeHandlingMode.valueOf(it) }.getOrNull() }
            ?: EpisodeHandlingMode.SHOW
        recapHandling = AnimeSettingsStorage.loadRecapHandling()
            ?.let { runCatching { EpisodeHandlingMode.valueOf(it) }.getOrNull() }
            ?: EpisodeHandlingMode.SHOW
        publish()
    }

    private fun publish() {
        _uiState.value = AnimeSettingsUiState(
            fillerHandling = fillerHandling,
            recapHandling = recapHandling,
        )
    }
}
