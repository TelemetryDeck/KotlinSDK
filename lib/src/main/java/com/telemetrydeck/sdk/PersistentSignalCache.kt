package com.telemetrydeck.sdk

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class PersistentSignalCache(
    private val cacheLimit: Int = 10_000,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
    private var signalQueue: MutableList<Signal> = mutableListOf(),
) : SignalCache {
    val cacheFileName: String = "telemetrydeck.json"
    private var file: File? = null

    private val ioScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val saveRequests = Channel<Unit>(Channel.CONFLATED)

    init {
        require(cacheLimit > 0) { "cacheLimit must be greater than zero" }
        ioScope.launch {
            for (request in saveRequests) {
                saveSignals()
            }
        }
    }

    constructor(
        cacheDir: File,
        logger: DebugLogger?,
        cacheLimit: Int = 10_000,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
    ) : this(cacheLimit = cacheLimit, ioDispatcher = ioDispatcher) {
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
                val trimmed = if (oldSignals.size > cacheLimit) {
                    logger?.debug("Restored cache exceeds cacheLimit ($cacheLimit), dropping ${oldSignals.size - cacheLimit} oldest signals")
                    oldSignals.takeLast(cacheLimit)
                } else {
                    oldSignals
                }
                logger?.debug("Restoring ${trimmed.count()} signals from cache.")
                signalQueue.addAll(trimmed)
            } catch (e: Exception) {
                logger?.error("Failed to parse signal cache.")
            }
        }
        file = writeFile
        scheduleSave()
    }

    override fun add(signal: Signal) {
        synchronized(this) {
            if (signalQueue.size >= cacheLimit) {
                val overflow = signalQueue.size - cacheLimit + 1
                repeat(overflow) { signalQueue.removeAt(0) }
            }
            signalQueue.add(signal)
        }
        scheduleSave()
    }

    override fun addAll(signals: List<Signal>) {
        if (signals.isEmpty()) return
        synchronized(this) {
            signalQueue.addAll(signals)
            if (signalQueue.size > cacheLimit) {
                val overflow = signalQueue.size - cacheLimit
                repeat(overflow) { signalQueue.removeAt(0) }
            }
        }
        scheduleSave()
    }

    override fun empty(): List<Signal> {
        val items: List<Signal>
        synchronized(this) {
            items = signalQueue.toList()
            signalQueue = mutableListOf()
        }
        scheduleSave()
        return items
    }

    override fun count(): Int = synchronized(this) { signalQueue.count() }

    private fun scheduleSave() {
        saveRequests.trySend(Unit)
    }

    private fun saveSignals() {
        val snapshot: List<Signal>
        synchronized(this) {
            snapshot = signalQueue.toList()
        }
        file?.parentFile?.mkdirs()
        file?.createNewFile()
        val json = Json.encodeToString(snapshot)
        file?.writeText(json)
    }
}
