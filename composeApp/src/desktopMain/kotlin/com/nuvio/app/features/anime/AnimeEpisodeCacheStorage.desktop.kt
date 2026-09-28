package com.nuvio.app.features.anime

import com.nuvio.app.core.storage.DesktopStorage

internal actual object AnimeEpisodeCacheStorage {
    private val store = DesktopStorage.store("nuvio_anime_cache")

    actual fun loadResolution(key: String): String? = store.getString("resolve_$key")

    actual fun saveResolution(key: String, json: String) {
        store.putString("resolve_$key", json)
    }

    actual fun loadEpisodes(malId: Int): String? = store.getString("episodes_$malId")

    actual fun saveEpisodes(malId: Int, json: String) {
        store.putString("episodes_$malId", json)
    }
}

internal actual fun animeCurrentTimeMs(): Long = System.currentTimeMillis()
