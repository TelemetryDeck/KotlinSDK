package com.telemetrydeck.sdk

/**
 * Generic interface for reading and writing the state kept by [TelemetryDeckProvider]s.
 */
interface TelemetryStorage {
    /**
     * Returns the value stored under [key], or `null` if no value is present.
     */
    fun read(key: String): String?

    /**
     * Stores [value] under [key], overwriting any previous value.
     */
    fun write(key: String, value: String)

    /**
     * Removes the value stored under [key], if any.
     */
    fun delete(key: String)
}
