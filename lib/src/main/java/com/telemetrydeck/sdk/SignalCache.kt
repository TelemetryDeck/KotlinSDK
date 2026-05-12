package com.telemetrydeck.sdk

interface SignalCache {
    /**
     * Appends a signal to the cache
     */
    fun add(signal: Signal)

    /**
     * Appends multiple signals to the cache in a single operation
     */
    fun addAll(signals: List<Signal>)

    /**
     * Empties the cache and returns all previously cached items
     */
    fun empty(): List<Signal>

    /**
     * Returns the number of items currently present in the queue
     */
    fun count(): Int
}