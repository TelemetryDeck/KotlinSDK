package com.telemetrydeck.sdk.providers

import android.content.Context
import com.telemetrydeck.sdk.TelemetryDeckIdentityProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.TelemetryStorage
import java.util.UUID

/**
 * [FileUserIdentityProvider] attempts to provide a stable user identifier across multiple sessions.
 *
 * When no user identifier has been provided to [com.telemetrydeck.sdk.TelemetryDeck], [FileUserIdentityProvider] uses a file
 * in the application's folder to store a randomly generated user identifier.
 *
 * The identifier will be removed when a user uninstalls an app. The KotlinSDK will not "bridge" the user's identity between installations.
 * Users can reset the identifier at any time by using the "Clear Data" action in Settings of their device.
 *
 *
 * */
class FileUserIdentityProvider: TelemetryDeckIdentityProvider {
    private var storage: TelemetryStorage? = null
    private val fileName = "telemetrydeckid"

    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        this.storage = client.storage
    }

    override fun stop() {
        // nothing to do here
    }

    override fun calculateIdentity(signalClientUser: String?, configurationDefaultUser: String?): String {
        val simpleValue = signalClientUser ?: configurationDefaultUser ?: readOrCreateStableIdentity() ?: ""
        return simpleValue
    }

    override fun resetIdentity() {
        // to reset, we delete the identity file if it exists
        storage?.delete(fileName)
    }

    private fun readOrCreateStableIdentity(): String? {
        val existingId = storage?.read(fileName)
        if (existingId != null) {
            return existingId
        }

        val newId = UUID.randomUUID().toString()
        storage?.write(fileName, newId)
        return newId
    }
}