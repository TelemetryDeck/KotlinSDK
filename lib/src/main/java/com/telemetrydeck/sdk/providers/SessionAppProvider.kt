package com.telemetrydeck.sdk.providers


import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.telemetrydeck.sdk.TelemetryDeckClient
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.signals.Session
import java.lang.ref.WeakReference

/**
 * Monitors the app lifecycle in order to broadcast the NewSessionBegan signal.
 */
class SessionAppProvider: TelemetryDeckProvider, DefaultLifecycleObserver {
    private var manager: WeakReference<TelemetryDeckClient>? = null

    override fun register(ctx: Application?, client: TelemetryDeckClient) {
        this.manager = WeakReference(client)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (manager?.get()?.configuration?.sendNewSessionBeganSignal == true) {
            manager?.get()?.signal(
                Session.Started.signalName
            )
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (manager?.get()?.configuration?.sendNewSessionBeganSignal == true) {
            // app is going into the background, reset the sessionID
            manager?.get()?.newSession()
        }
    }
}