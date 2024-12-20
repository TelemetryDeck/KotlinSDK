package com.telemetrydeck.sdk

import android.app.Application

/**
 * Generic interface for plugins which can calculate anonymous user identifier
 */
interface TelemetryDeckIdentityProvider {
    /**
     * Registers the provider with the telemetry manager.
     */
    fun register(ctx: Application?, client: TelemetryDeckSignalProcessor)

    /**
     * Calling stop deactivates the provider, performs any cleanup work if necessary.
     */
    fun stop()


    /**
     * Calculate the user identifier to be attached to a signal.
     *
     * @param[signalClientUser] The existing `clientUser` value of the signal (if any has been provided).
     * @param[configurationDefaultUser] The default user value from the [TelemetryDeck] configuration (if any has been provided).
     * @return the user identifier to be associated with the outgoing signal.
     */
    fun calculateIdentity(signalClientUser: String?, configurationDefaultUser: String?): String

    /**
     * Permanently removes any persisted user identifier.
     */
    fun resetIdentity()
}