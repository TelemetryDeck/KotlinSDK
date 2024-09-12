package com.telemetrydeck.sdk

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.lang.ref.WeakReference

/**
 * Monitors the app lifecycle in order to broadcast the NewSessionBegan signal.
 */
class SessionProvider: TelemetryProvider, DefaultLifecycleObserver {
    private var manager: WeakReference<TelemetryManagerSignals>? = null

    override fun register(ctx: Application?, manager: TelemetryManagerSignals) {
        this.manager = WeakReference(manager)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (manager?.get()?.configuration?.sendNewSessionBeganSignal == true) {
            manager?.get()?.queue(
                SignalType.NewSessionBegan
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