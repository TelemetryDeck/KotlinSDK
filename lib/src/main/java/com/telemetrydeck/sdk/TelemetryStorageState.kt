package com.telemetrydeck.sdk

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Reads and decodes the state stored under [key], or `null` if no value is present or decoding fails.
 */
internal inline fun <reified T> TelemetryStorage.readState(key: String, logger: DebugLogger?): T? {
    return try {
        val json = read(key) ?: return null
        Json.decodeFromString<T>(json)
    } catch (e: Exception) {
        logger?.error("failed to restore state ${e.stackTraceToString()}")
        null
    }
}

/**
 * Encodes [state] and stores it under [key].
 */
internal inline fun <reified T> TelemetryStorage.writeState(key: String, state: T, logger: DebugLogger?) {
    try {
        val json = Json.encodeToString(state)
        write(key, json)
    } catch (e: Exception) {
        logger?.error("failed to write state ${e.stackTraceToString()}")
    }
}
