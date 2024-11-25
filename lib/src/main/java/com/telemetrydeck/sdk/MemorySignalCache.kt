package com.telemetrydeck.sdk

class MemorySignalCache(private var signalQueue: MutableList<Signal> = mutableListOf()) :
    SignalCache {

    override fun add(signal: Signal) {
        synchronized(this) {
            signalQueue.add(signal)
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