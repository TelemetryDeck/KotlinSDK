package com.telemetrydeck.sdk

/**
 * Used to output internal debug logs
 */
interface DebugLogger {
    /**
     * Logs a message with the Error level.
     * This is used when something unexpected happens or certain functionality becomes unavailable
     */
    fun error(message: String)

    /**
     * Logs a message with the Debug level
     */
    fun debug(message: String)

    /**
     * Enable or disable future logging
     */
    fun configure(enabled: Boolean)
}