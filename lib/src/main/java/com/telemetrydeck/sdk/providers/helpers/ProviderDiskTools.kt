package com.telemetrydeck.sdk.providers.helpers

import android.content.Context
import com.telemetrydeck.sdk.DebugLogger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.Charset


internal inline fun <reified T> restoreStateFromDisk(
    appContext: Context?,
    fileName: String,
    fileEncoding: Charset = Charsets.UTF_8,
    logger: DebugLogger?
): T? {
    val context = appContext ?: return null
    try {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            val json = file.readText(fileEncoding)
            val state = Json.decodeFromString<T>(json)
            return state
        }
    } catch (e: Exception) {
        logger?.error("failed to restore state ${e.stackTraceToString()}")
    }
    return null
}

internal inline fun <reified T> writeStateToDisk(state: T,
                                                 appContext: Context?,
                                                 fileName: String,
                                                 fileEncoding: Charset = Charsets.UTF_8,
                                                 logger: DebugLogger?) {
    val context = appContext ?: return
    try {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }
        val json = Json.encodeToString(state)
        file.writeText(json, fileEncoding)
    } catch (e: Exception) {
        logger?.error("failed to write state ${e.stackTraceToString()}")
    }
}