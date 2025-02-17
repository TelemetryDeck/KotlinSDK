package com.telemetrydeck.sdk

import java.util.UUID

interface TelemetryDeckClient {

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
    fun navigate(destinationPath: String, customUserID: String? = null)

    /**
     * Send a `TelemetryDeck.Acquisition.userAcquired` signal with the provided channel.
     */
    fun acquiredUser(channel: String, params: Map<String, String> = emptyMap(), customUserID: String? = null)


    /**
     * Send a `TelemetryDeck.Acquisition.leadStarted` signal with the provided leadId.
     */
    fun leadStarted(leadId: String, params: Map<String, String> = emptyMap(), customUserID: String? = null)


    /**
     * Send a `TelemetryDeck.Acquisition.leadConverted` signal with the provided leadId.
     */
    fun leadConverted(leadId: String, params: Map<String, String> = emptyMap(), customUserID: String? = null)


    /**
     * Send a signal immediately
     */
    suspend fun send(
        signalType: String,
        clientUser: String? = null,
        additionalPayload: Map<String, String> = emptyMap(),
        floatValue: Double? = null,
    ): Result<Unit>


    /**
     *  Sends a telemetry signal with optional parameters to TelemetryDeck.
     *
     *
     *  Signals are first queued in cache (see [SignalCache]) before being sent to the server.
     *  In case of failure, we will try sending again approximately every 10 seconds while the app is running.
     *
     *  When running in the context of an application, the signal cache is written to a local file so signals are saved when the app restarts (see [PersistentSignalCache]).
     *  When running without a context, the signal cache is stored in memory. All cached (unsent) signals are discarded when the TelemetryDeck SDK instance has been disposed (see [MemorySignalCache]).
     *
     *
     *  If you prefer to control the lifecycle of signals, use the [send] method instead.
     *
     * @param signalName The name of the signal to be sent. This is a string that identifies the type of event or action being reported.
     * @param params A map of additional string key-value pairs that provide further context about the signal.
     * @param floatValue An optional floating-point number that can be used to provide numerical data about the signal.
     * @param customUserID An optional string specifying a custom user identifier. If provided, it will override the default user identifier from the configuration.
     *
     */
    fun signal(
        signalName: String,
        params: Map<String, String> = emptyMap(),
        floatValue: Double? = null,
        customUserID: String? = null,
    )

    /**
     *  Sends a telemetry signal with optional parameters to TelemetryDeck.
     *
     *
     *  Signals are first queued in cache (see [SignalCache]) before being sent to the server.
     *  In case of failure, we will try sending again approximately every 10 seconds while the app is running.
     *
     *  When running in the context of an application, the signal cache is written to a local file so signals are saved when the app restarts (see [PersistentSignalCache]).
     *  When running without a context, the signal cache is stored in memory. All cached (unsent) signals are discarded when the TelemetryDeck SDK instance has been disposed (see [MemorySignalCache]).
     *
     *
     *  If you prefer to control the lifecycle of signals, use the [send] method instead.
     *
     * @param signalName The name of the signal to be sent. This is a string that identifies the type of event or action being reported.
     * @param customUserID An optional string specifying a custom user identifier. If provided, it will override the default user identifier from the configuration.
     * @param params A map of additional string key-value pairs that provide further context about the signal.
     *
     */
    fun signal(
        signalName: String,
        customUserID: String? = null,
        params: Map<String, String> = emptyMap(),
    )

    /** Starts tracking the duration of a signal without sending it yet.
     *
     * This function only starts tracking time – it does not send a signal. You must call `[stopAndSendDurationSignal]`
     * with the same signal name to finalize and actually send the signal with the tracked duration.
     *
     * Calling this method twice for the same signal name will replace the previous tracking.
     *
     * @param signalName The name of the signal to track. This will be used to identify and stop the duration tracking later.
     * @param parameters A dictionary of additional string key-value pairs that will be included when the duration signal is eventually sent.
     */
    fun startDurationSignal(signalName: String, parameters: Map<String, String> = emptyMap())

    /** Stops tracking the duration of a signal and sends it with the total duration.
     *
     * This function finalizes the duration tracking by:
     * 1. Stopping the timer for the given signal name
     * 2. Calculating the duration in seconds (excluding background time)
     * 3. Sending a signal that includes the start parameters, stop parameters, and calculated duration
     *
     * Tracked time will be included as a value of `TelemetryDeck.Signal.durationInSeconds`.
     * Tracked time only includes time while the app is in the foreground.
     *
     * Note: If no matching signal was started, this function has no effect.
     *
     * @param signalName The name of the signal that was previously started with [startDurationSignal]
     * @param parameters Additional parameters to include with the signal. These will be merged with the parameters provided at the start.
     *
     */
    fun stopAndSendDurationSignal(signalName: String, parameters: Map<String, String> = emptyMap())

    val configuration: TelemetryManagerConfiguration?
}