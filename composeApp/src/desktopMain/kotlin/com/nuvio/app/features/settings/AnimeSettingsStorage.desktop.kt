package com.nuvio.app.features.settings

import com.nuvio.app.core.storage.DesktopStorage

internal actual object AnimeSettingsStorage {
    private const val fillerHandlingKey = "filler_handling"
    private const val recapHandlingKey = "recap_handling"
    private val store = DesktopStorage.store("nuvio_anime_settings")

    actual fun loadFillerHandling(): String? = store.getString(fillerHandlingKey)

    actual fun saveFillerHandling(mode: String) {
        store.putString(fillerHandlingKey, mode)
    }

    actual fun loadRecapHandling(): String? = store.getString(recapHandlingKey)

    actual fun saveRecapHandling(mode: String) {
        store.putString(recapHandlingKey, mode)
    }
}
