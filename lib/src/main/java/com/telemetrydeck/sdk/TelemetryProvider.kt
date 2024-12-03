package com.telemetrydeck.sdk

import android.app.Application

/**
 * New providers should adopt this interface to be able to be used with the legacy TelemetryManager.
 *
 * To preserve backwards compatibility with TelemetryManager, this interface is a copy of [TelemetryDeckProvider] with all methods prefixed with `fallback`.
 *
 * This interface should remain internal!
 */
@Deprecated("Use TelemetryDeckProvider")
internal interface TelemetryProviderFallback {

    fun fallbackRegister(ctx: Application?, client: TelemetryDeckSignalProcessor)

    fun fallbackStop()

    fun fallbackEnrich(
        signalType: String,
        clientUser: String? = null,
        additionalPayload: Map<String, String> = emptyMap()
    ): Map<String, String> {
        return additionalPayload
    }
}

/**
 * Generic interface for plugins which can create Signals
 */
@Deprecated("Use TelemetryDeckProvider", ReplaceWith("TelemetryDeckProvider"))
interface TelemetryProvider {
    /**
     * Registers the provider with the telemetry manager.
     * The provider keeps a weak reference to telemetry manager in order to queue or send signals.
     */
    fun register(ctx: Application?, manager: TelemetryManager)

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
    fun enrich(
        signalType: String,
        clientUser: String? = null,
        additionalPayload: Map<String, String> = emptyMap()
    ): Map<String, String> {
        return additionalPayload
    }
}