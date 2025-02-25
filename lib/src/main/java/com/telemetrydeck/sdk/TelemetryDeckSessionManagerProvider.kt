package com.telemetrydeck.sdk

import java.util.UUID

/**
 * Generic interface for plugins which can offer session management.
 */
interface TelemetryDeckSessionManagerProvider: TelemetryDeckProvider {

    /**
     * Returns the current session ID.
     *
     * Returns `null` when session tracking is unavailable.
     * */
    fun getCurrentSessionID(): UUID?


    /**
     * Calling this method ends the current session and generates a new session.
     */
    fun startNewSession(sessionID: UUID = UUID.randomUUID())

    /**
     * Sets the sessionID to be used when the plugin is started.
     *
     * Note: this method is called exactly once before the plugin is started.
     */
    fun setFirstSessionID(sessionID: UUID)
}