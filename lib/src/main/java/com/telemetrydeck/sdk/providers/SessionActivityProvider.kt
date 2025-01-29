package com.telemetrydeck.sdk.providers

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.telemetrydeck.sdk.SignalType
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.TelemetryProviderFallback
import java.lang.ref.WeakReference

/**
 * Emits signals for application and activity lifecycle events.
 */
class SessionActivityProvider : TelemetryDeckProvider,
    Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver, TelemetryProviderFallback {
    private var manager: WeakReference<TelemetryDeckSignalProcessor>? = null

    override fun fallbackRegister(ctx: Application?, client: TelemetryDeckSignalProcessor) {
        register(ctx, client)
    }

    override fun fallbackStop() {
        stop()
    }

    override fun register(ctx: Application?, client: TelemetryDeckSignalProcessor) {
        this.manager = WeakReference(client)
        if (ctx == null) {
            this.manager?.get()?.debugLogger?.error("AppLifecycleTelemetryProvider requires a context but received null. No signals will be sent.")
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        ctx?.registerActivityLifecycleCallbacks(this)
    }

    override fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        manager?.clear()
        manager = null
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        manager?.get()?.processSignal(
            SignalType.ActivityCreated.type,
            mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivityStarted(p0: Activity) {
        manager?.get()?.processSignal(
            SignalType.ActivityStarted.type,
            mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivityResumed(p0: Activity) {
        manager?.get()?.processSignal(
            SignalType.ActivityResumed.type,
            mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivityPaused(p0: Activity) {
        manager?.get()?.processSignal(
            SignalType.ActivityPaused.type,
            mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivityStopped(p0: Activity) {
        manager?.get()?.processSignal(
            SignalType.ActivityStopped.type,
            mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        manager?.get()?.processSignal(
            SignalType.ActivitySaveInstanceState.type,
            mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivityDestroyed(p0: Activity) {
        manager?.get()?.processSignal(
            SignalType.ActivityDestroyed.type,
            mapOf("activity" to p0.localClassName)
        )
    }

    override fun onStart(owner: LifecycleOwner) {
        manager?.get()?.processSignal(
            SignalType.AppForeground.type
        )
    }

    override fun onStop(owner: LifecycleOwner) {
        manager?.get()?.processSignal(
            SignalType.AppBackground.type
        )
    }


}