package com.telemetrydeck.sdk

import java.util.UUID

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
     * Send a signal that represents a navigation event with a source and a destination.
     *
     * @see <a href="https://telemetrydeck.com/docs/articles/navigation-signals/">Navigation Signals</a>
     * */
    fun navigate(sourcePath: String, destinationPath: String, clientUser: String? = null)

    /**
     * Send a signal that represents a navigation event with a destination and a default source.
     *
     * @see <a href="https://telemetrydeck.com/docs/articles/navigation-signals/">Navigation Signals</a>
     * */
    fun navigate(destinationPath: String, clientUser: String? = null)


    /**
     * Send a signal immediately
     */
    suspend fun send(
        signalType: String,
        clientUser: String? = null,
        additionalPayload: Map<String, String> = emptyMap()
    ): Result<Unit>


    /**
     * Send a signal immediately
     */
    suspend fun send(
        signalType: SignalType,
        clientUser: String? = null,
        additionalPayload: Map<String, String> = emptyMap()
    ): Result<Unit>
}