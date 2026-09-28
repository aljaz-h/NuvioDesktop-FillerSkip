package com.nuvio.app.features.settings

internal expect object AnimeSettingsStorage {
    fun loadFillerHandling(): String?
    fun saveFillerHandling(mode: String)
    fun loadRecapHandling(): String?
    fun saveRecapHandling(mode: String)
}
