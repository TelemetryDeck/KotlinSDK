package com.telemetrydeck.sdk.providers

import android.content.Context
import com.telemetrydeck.sdk.TelemetryDeckIdentityProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import java.io.File
import java.lang.ref.WeakReference
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
    private var app: WeakReference<Context?>? = null
    private var manager: WeakReference<TelemetryDeckSignalProcessor>? = null
    private val fileName = "telemetrydeckid"
    private val fileEncoding = Charsets.UTF_8

    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        this.app = WeakReference(ctx)
        this.manager = WeakReference(client)
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
        val context = this.app?.get() ?: return
        val file = File(context.filesDir, fileName)
        try {
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            this.manager?.get()?.debugLogger?.error(e.stackTraceToString())
        }
    }

    private fun readOrCreateStableIdentity(): String? {
        val context = this.app?.get() ?: return null

        try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) {
                val newId = UUID.randomUUID().toString()
                file.writeText(newId, fileEncoding)
                return newId
            }

            return file.readText(fileEncoding)
        } catch (e: Exception) {
            this.manager?.get()?.debugLogger?.error(e.stackTraceToString())
            return null
        }
    }
}