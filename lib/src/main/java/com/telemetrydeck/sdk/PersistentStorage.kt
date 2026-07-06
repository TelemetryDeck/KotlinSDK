package com.telemetrydeck.sdk

import java.io.File

/**
 * [PersistentStorage] reads and writes provider state to files inside [filesDir].
 */
class PersistentStorage(
    private val filesDir: File,
    private val logger: DebugLogger?,
) : TelemetryStorage {
    private val fileEncoding = Charsets.UTF_8

    override fun read(key: String): String? {
        try {
            val file = File(filesDir, key)
            if (file.exists()) {
                return file.readText(fileEncoding)
            }
        } catch (e: Exception) {
            logger?.error("failed to restore state ${e.stackTraceToString()}")
        }
        return null
    }

    override fun write(key: String, value: String) {
        try {
            val file = File(filesDir, key)
            file.writeText(value, fileEncoding)
        } catch (e: Exception) {
            logger?.error("failed to write state ${e.stackTraceToString()}")
        }
    }

    override fun delete(key: String) {
        try {
            val file = File(filesDir, key)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            logger?.error("failed to delete state ${e.stackTraceToString()}")
        }
    }
}
