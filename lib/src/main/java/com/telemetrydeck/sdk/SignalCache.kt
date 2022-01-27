package com.telemetrydeck.sdk

interface SignalCache {
    /**
     * Appends a signal to the cache
     */
    fun add(signal: Signal)

    /**
     * Empties the cache and returns all previously cached items
     */
    fun empty(): List<Signal>

    /**
     * Returns the number of items currently present in the queue
     */
    fun count(): Int
}