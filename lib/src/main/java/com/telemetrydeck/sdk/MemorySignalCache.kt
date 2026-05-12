package com.telemetrydeck.sdk

class MemorySignalCache(
    private val cacheLimit: Int = Int.MAX_VALUE,
    private var signalQueue: MutableList<Signal> = mutableListOf(),
) : SignalCache {

    init {
        require(cacheLimit > 0) { "cacheLimit must be greater than zero" }
    }

    override fun add(signal: Signal) {
        synchronized(this) {
            if (signalQueue.size >= cacheLimit) {
                val overflow = signalQueue.size - cacheLimit + 1
                repeat(overflow) { signalQueue.removeAt(0) }
            }
            signalQueue.add(signal)
        }
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
    }

    override fun empty(): List<Signal> {
        synchronized(this) {
            val items = signalQueue.toList()
            signalQueue = mutableListOf()
            return items
        }
    }

    override fun count(): Int {
        synchronized(this) {
            return signalQueue.count()
        }
    }
}