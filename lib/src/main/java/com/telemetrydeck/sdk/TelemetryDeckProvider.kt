package com.telemetrydeck.sdk

import android.content.Context

/**
 * Generic interface for plugins which can enrich Signals
 */
interface TelemetryDeckProvider {
    /**
     * Registers the provider with the telemetry manager.
     * The provider may keep a weak reference to the application context and the telemetry manager in order to queue or send signals.
     */
    fun register(ctx: Context?, client: TelemetryDeckSignalProcessor)

    /**
     * Calling stop deactivates the provider and prevents future signals from being sent.
     */
    fun stop()

    /**
     * A provider can override this method in order to append or remove telemetry metadata from Signals
     * before they are enqueued for broadcast.
     *
     * [TelemetryDeck] calls this method on all providers in order of registration.
     */
    fun enrich(
        signalType: String,
        clientUser: String? = null,
        additionalPayload: Map<String, String> = emptyMap()
    ): Map<String, String> {
        return additionalPayload
    }

    /**
     * A provider can override this method in order apply a transformation on any part of the signal, including the signal name.
     * [TelemetryDeck] calls this method on all providers in order of registration.
     *
     * This method is always called **after** [enrich].
     */
    fun transform(
        signalTransform: SignalTransform
    ): SignalTransform {
        return signalTransform
    }
}

