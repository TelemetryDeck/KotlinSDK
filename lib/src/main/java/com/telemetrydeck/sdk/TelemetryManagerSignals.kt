package com.telemetrydeck.sdk

interface TelemetryManagerSignals {

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