package com.telemetrydeck.sdk

import android.app.Application

/**
 * Generic interface for plugins which can create Signals
 */
interface TelemetryProvider {
    /**
     * Registers the provider with the telemetry manager.
     * The provider keeps a weak reference to telemetry manager in order to queue or send signals.
     */
    fun register(ctx: Application?, manager: TelemetryDeckClient)

    /**
     * Calling stop deactivates the provider and prevents future signals from being sent.
     */
    fun stop()

    /**
     * A provider can override this method in order to append or remove telemetry metadata from Signals
     * before they are enqueued for broadcast.
     *
     * TelemetryManager calls this method all providers in order of registration.
     */
    fun enrich(signalType: String,
               clientUser: String? = null,
               additionalPayload: Map<String, String> = emptyMap()): Map<String, String> {
        return additionalPayload
    }
}