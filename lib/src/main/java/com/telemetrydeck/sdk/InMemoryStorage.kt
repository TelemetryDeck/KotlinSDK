package com.telemetrydeck.sdk

/**
 * [InMemoryStorage] keeps provider state in memory for the lifetime of the process without persisting anything to disk.
 */
class InMemoryStorage : TelemetryStorage {
    private val values = mutableMapOf<String, String>()

    override fun read(key: String): String? {
        synchronized(this) {
            return values[key]
        }
    }

    override fun write(key: String, value: String) {
        synchronized(this) {
            values[key] = value
        }
    }

    override fun delete(key: String) {
        synchronized(this) {
            values.remove(key)
        }
    }
}
