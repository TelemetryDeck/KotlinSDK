package com.telemetrydeck.sdk

import java.util.*

interface TelemetryManagerSignals {

    /**
     * All future signals belong to a new session.
     *
     * Calling this method sets a new SessionID for new Signals. Previously queued signals are not affected.
     */
    fun newSession(sessionID: UUID = UUID.randomUUID())


    /**
     * Set the default user for future signals
     *
     */
    fun newDefaultUser(user: String?)


    /**
     * Queue a signal to be send as soon as possible
     */
    fun queue(
        signalType: String,
        clientUser: String? = null,
        additionalPayload: Map<String, String> = emptyMap()
    )


    /**
     * Queue a signal to be send as soon as possible
     */
    fun queue(
        signalType: SignalType,
        clientUser: String? = null,
        additionalPayload: Map<String, String> = emptyMap()
    )


    /**
     * Send a signal immediately
     */
    suspend fun send(
        signalType: String,
        clientUser: String? = null,
        additionalPayload: Map<String, String> = emptyMap()
    )


    /**
     * Send a signal immediately
     */
    suspend fun send(
        signalType: SignalType,
        clientUser: String? = null,
        additionalPayload: Map<String, String> = emptyMap()
    )
}