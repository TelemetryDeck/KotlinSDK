package com.telemetrydeck.sdk.providers


import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.signals.Session
import java.lang.ref.WeakReference

/**
 * Monitors the app lifecycle in order to broadcast the NewSessionBegan signal.
 *
 * @deprecated Use [SessionTrackingSignalProvider] instead.
 */
@Deprecated(
    message = "Use SessionTrackingSignalProvider instead.",
    replaceWith = ReplaceWith("SessionTrackingSignalProvider"),
    level = DeprecationLevel.WARNING
)
class SessionAppProvider : TelemetryDeckProvider, DefaultLifecycleObserver {
    private var manager: WeakReference<TelemetryDeckSignalProcessor>? = null

    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
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