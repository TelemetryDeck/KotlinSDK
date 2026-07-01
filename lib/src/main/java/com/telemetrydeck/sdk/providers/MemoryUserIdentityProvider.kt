package com.telemetrydeck.sdk.providers

import android.content.Context
import com.telemetrydeck.sdk.TelemetryDeckIdentityProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import java.util.UUID

/**
 * [MemoryUserIdentityProvider] generates a stable anonymous user identifier for the lifetime of
 * the process without persisting anything to disk.
 */
class MemoryUserIdentityProvider : TelemetryDeckIdentityProvider {
    private val sessionIdentity: String = UUID.randomUUID().toString()

    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
    }

    override fun stop() {
    }

    override fun calculateIdentity(signalClientUser: String?, configurationDefaultUser: String?): String {
        return signalClientUser ?: configurationDefaultUser ?: sessionIdentity
    }

    override fun resetIdentity() {
    }
}
