package com.nuvio.app.features.anime

internal actual fun tenraiDebugMalIdOverride(): Int? =
    System.getenv("NUVIO_TENRAI_DEBUG_MAL_ID")?.trim()?.toIntOrNull()
