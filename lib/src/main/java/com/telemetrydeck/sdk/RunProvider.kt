package com.telemetrydeck.sdk

internal inline fun <T> runProvider(provider: Any, logger: DebugLogger?, fallback: T, block: () -> T): T {
    return try {
        block()
    } catch (e: Exception) {
        logger?.error("Provider ${provider::class} failed: $e ${e.stackTraceToString()}")
        fallback
    }
}
