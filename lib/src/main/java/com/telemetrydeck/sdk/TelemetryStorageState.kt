package com.telemetrydeck.sdk

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Reads and decodes the state stored under [key], or `null` if no value is present or decoding fails.
 */
internal inline fun <reified T> TelemetryStorage.readState(key: String, logger: DebugLogger?): T? {
    val json = read(key) ?: return null
    return try {
        Json.decodeFromString<T>(json)
    } catch (e: Exception) {
        logger?.error("failed to restore state ${e.stackTraceToString()}")
        null
    }
}

/**
 * Encodes [state] and stores it under [key].
 */
internal inline fun <reified T> TelemetryStorage.writeState(key: String, state: T) {
    val json = Json.encodeToString(state)
    write(key, json)
}
