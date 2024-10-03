package com.telemetrydeck.sdk.providers

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.telemetrydeck.sdk.SignalType
import com.telemetrydeck.sdk.TelemetryDeckClient
import com.telemetrydeck.sdk.TelemetryDeckProvider
import java.lang.ref.WeakReference

/**
 * Emits signals for application and activity lifecycle events.
 */
class SessionActivityProvider: TelemetryDeckProvider,
    Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private var manager: WeakReference<TelemetryDeckClient>? = null

    override fun register(ctx: Application?, client: TelemetryDeckClient) {
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
        manager?.get()?.signal(
            SignalType.ActivityCreated,
            mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivityStarted(p0: Activity) {
        manager?.get()?.signal(
            SignalType.ActivityStarted,
            mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivityResumed(p0: Activity) {
        manager?.get()?.signal(
            SignalType.ActivityResumed,
            mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivityPaused(p0: Activity) {
        manager?.get()?.signal(
            SignalType.ActivityPaused,
            mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivityStopped(p0: Activity) {
        manager?.get()?.signal(
            SignalType.ActivityStopped,
            mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        manager?.get()?.signal(
            SignalType.ActivitySaveInstanceState,
            mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivityDestroyed(p0: Activity) {
        manager?.get()?.signal(
            SignalType.ActivityDestroyed,
            mapOf("activity" to p0.localClassName)
        )
    }

    override fun onStart(owner: LifecycleOwner) {
        manager?.get()?.signal(
            SignalType.AppForeground
        )
    }

    override fun onStop(owner: LifecycleOwner) {
        manager?.get()?.signal(
            SignalType.AppBackground
        )
    }
}