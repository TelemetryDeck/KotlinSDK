package com.telemetrydeck.sdk.providers


import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.TelemetryProviderFallback
import com.telemetrydeck.sdk.signals.Session
import java.lang.ref.WeakReference

/**
 * Monitors the app lifecycle in order to broadcast the NewSessionBegan signal.
 */
internal class SessionAppProvider : TelemetryDeckProvider, DefaultLifecycleObserver,
    TelemetryProviderFallback {
    private var manager: WeakReference<TelemetryDeckSignalProcessor>? = null

    override fun fallbackRegister(ctx: Application?, client: TelemetryDeckSignalProcessor) {
        register(ctx, client)
    }

    override fun fallbackStop() {
        stop()
    }

    override fun register(ctx: Application?, client: TelemetryDeckSignalProcessor) {
        this.manager = WeakReference(client)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }


    override fun onStart(owner: LifecycleOwner) {
        if (manager?.get()?.configuration?.sendNewSessionBeganSignal == true) {
            manager?.get()?.processSignal(
                Session.Started.signalName
            )
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (manager?.get()?.configuration?.sendNewSessionBeganSignal == true) {
            // app is going into the background, reset the sessionID
            manager?.get()?.resetSession()
        }
    }

}