package com.telemetrydeck.sdk

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class PersistentSignalCache(private var signalQueue: MutableList<Signal> = mutableListOf()) :
    SignalCache {
    val cacheFileName: String = "telemetrydeck.json"
    private var file: File? = null

    constructor(cacheDir: File, logger: DebugLogger?) : this() {
        if (!cacheDir.isDirectory) {
            logger?.error("Expected a folder but received a file instead.")
            return
        }

        val writeFile = File(cacheDir, cacheFileName)
        logger?.debug("Using signal cache at ${writeFile.absolutePath}.")
        if (writeFile.exists()) {
            logger?.debug("Detected existing signal cache, attempting to parse...")
            val content = writeFile.readText()
            try {
                val oldSignals: List<Signal> = Json.decodeFromString(content)
                logger?.error("Restoring ${oldSignals.count()} signals from cache.")
                signalQueue.addAll(oldSignals)
            } catch (e: Exception) {
                logger?.error("Failed to parse signal cache.")
            }
        }
        file = writeFile
        saveSignals()
    }

    override fun add(signal: Signal) {
        synchronized(this) {
            signalQueue.add(signal)
            saveSignals()
        }

    }

    override fun empty(): List<Signal> {
        synchronized(this) {
            val items = signalQueue.toList()
            signalQueue = mutableListOf()
            saveSignals()
            return items
        }
    }

    override fun count(): Int {
        synchronized(this) {
            return signalQueue.count()
        }
    }

    private fun saveSignals() {
        file?.createNewFile()
        val json = Json.encodeToString(signalQueue)
        file?.writeText(json)
    }
}