package com.nuvio.app.features.anime

object TenraiConfig {
    const val BASE_URL = "https://api.tenrai.org/v1"
}

/**
 * Development-only override that forces the MAL id used for the currently opened anime
 * candidate, so filler/recap classification can be verified independently of title
 * matching. Reads the `NUVIO_TENRAI_DEBUG_MAL_ID` environment variable; unset by default,
 * so it never affects a normal run. Never set this in a release/distributed build.
 */
internal expect fun tenraiDebugMalIdOverride(): Int?
