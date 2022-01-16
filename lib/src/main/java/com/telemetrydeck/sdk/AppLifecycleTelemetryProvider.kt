package com.telemetrydeck.sdk

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import java.lang.ref.WeakReference

class AppLifecycleTelemetryProvider : ComponentCallbacks2, TelemetryProvider {
    private var manager: WeakReference<TelemetryManager>? = null

    override fun register(ctx: Application?, manager: TelemetryManager) {
        this.manager = WeakReference(manager)
        ctx?.registerComponentCallbacks(this)
        if (ctx == null) {
            this.manager?.get()?.logger?.error(
                "AppLifecycleTelemetryProvider requires a context but received null. No signals will be sent."
            )
        }
    }

    override fun stop() {
        manager?.clear()
        manager = null
    }

    override fun onConfigurationChanged(p0: Configuration) {
        manager?.get()?.queue(SignalType.ConfigurationChanged)
    }

    override fun onLowMemory() {
        manager?.get()?.queue(SignalType.LowMemory)
    }

    override fun onTrimMemory(p0: Int) {
        if (p0 == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            manager?.get()?.queue(SignalType.AppBackgrounded)
        }
    }


}