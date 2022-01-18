package com.telemetrydeck.sdk

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.lang.ref.WeakReference

/**
 * Emits signals for application and activity lifecycle events.
 */
class AppLifecycleTelemetryProvider : TelemetryProvider,
    Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private var manager: WeakReference<TelemetryManager>? = null

    override fun register(ctx: Application?, manager: TelemetryManager) {
        if (ctx == null) {
            this.manager?.get()?.logger?.error("ActivityLifecycleTelemetryProvider requires a context but received null. No signals will be sent.")
        }
        this.manager = WeakReference(manager)
        ctx?.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this);
    }

    override fun stop() {
        manager?.clear()
        manager = null
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        manager?.get()?.queue(
            SignalType.ActivityCreated,
            additionalPayload = mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivityStarted(p0: Activity) {
        manager?.get()?.queue(
            SignalType.ActivityStarted,
            additionalPayload = mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivityResumed(p0: Activity) {
        manager?.get()?.queue(
            SignalType.ActivityResumed,
            additionalPayload = mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivityPaused(p0: Activity) {
        manager?.get()?.queue(
            SignalType.ActivityPaused,
            additionalPayload = mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivityStopped(p0: Activity) {
        manager?.get()?.queue(
            SignalType.ActivityStopped,
            additionalPayload = mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
        manager?.get()?.queue(
            SignalType.ActivitySaveInstanceState,
            additionalPayload = mapOf("activity" to p0.localClassName)
        )
    }

    override fun onActivityDestroyed(p0: Activity) {
        manager?.get()?.queue(
            SignalType.ActivityDestroyed,
            additionalPayload = mapOf("activity" to p0.localClassName)
        )
    }

    override fun onStart(owner: LifecycleOwner) {
        manager?.get()?.queue(
            SignalType.AppForeground
        )
    }

    override fun onStop(owner: LifecycleOwner) {
        manager?.get()?.queue(
            SignalType.AppBackground
        )
    }
}