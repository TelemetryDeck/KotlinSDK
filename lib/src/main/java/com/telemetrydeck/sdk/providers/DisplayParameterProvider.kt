package com.telemetrydeck.sdk.providers

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import androidx.annotation.RequiresApi
import com.telemetrydeck.sdk.DebugLogger
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.params.Screens
import com.telemetrydeck.sdk.platform.DisplaySize
import com.telemetrydeck.sdk.platform.getConnectedDisplaySizes
import java.lang.ref.WeakReference

/**
 * Enriches signals with the native pixel dimensions of connected displays.
 */
@RequiresApi(Build.VERSION_CODES.N)
class DisplayParameterProvider internal constructor(
    private val now: () -> Long
) : TelemetryDeckProvider {
    constructor() : this(System::currentTimeMillis)

    private var app: WeakReference<Context?>? = null
    private var manager: WeakReference<TelemetryDeckSignalProcessor>? = null
    private var displayManager: WeakReference<DisplayManager?>? = null
    private var displayListener: DisplayManager.DisplayListener? = null

    private var cachedParams: Map<String, String>? = null
    private var cacheTimestamp: Long = 0L

    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        this.app = WeakReference(ctx)
        this.manager = WeakReference(client)

        if (ctx == null) {
            this.manager?.get()?.debugLogger?.error("DisplayParameterProvider requires a context but received null. Signals will not contain screen metadata.")
            return
        }

        try {
            val displayManager = ctx.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            this.displayManager = WeakReference(displayManager)

            val listener = object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) = invalidateCache()
                override fun onDisplayRemoved(displayId: Int) = invalidateCache()
                override fun onDisplayChanged(displayId: Int) = invalidateCache()
            }
            displayManager?.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
            this.displayListener = listener
        } catch (e: Exception) {
            this.manager?.get()?.debugLogger?.error("DisplayParameterProvider failed to register display listener: $e ${e.stackTraceToString()}")
        }
    }

    override fun stop() {
        try {
            val listener = this.displayListener ?: return
            this.displayManager?.get()?.unregisterDisplayListener(listener)
        } catch (e: Exception) {
            this.manager?.get()?.debugLogger?.error("DisplayParameterProvider failed to unregister display listener: $e ${e.stackTraceToString()}")
        } finally {
            this.displayListener = null
        }
    }

    override fun enrich(
        signalType: String,
        clientUser: String?,
        additionalPayload: Map<String, String>
    ): Map<String, String> {
        val signalPayload = additionalPayload.toMutableMap()
        for (item in resolvedParams()) {
            if (!signalPayload.containsKey(item.key)) {
                signalPayload[item.key] = item.value
            }
        }
        return signalPayload
    }

    @Synchronized
    internal fun resolvedParams(): Map<String, String> {
        val isTestMode = this.manager?.get()?.configuration?.testMode == true
        val cached = cachedParams
        if (!isTestMode && cached != null && now() - cacheTimestamp < CACHE_LIFETIME_MILLIS) {
            return cached
        }

        val ctx = this.app?.get() ?: return cached ?: emptyMap()
        val fresh = readScreenParams(ctx, this.manager?.get()?.debugLogger)
        cachedParams = fresh
        cacheTimestamp = now()
        return fresh
    }

    @Synchronized
    private fun invalidateCache() {
        cachedParams = null
    }

    private fun readScreenParams(context: Context, logger: DebugLogger?): Map<String, String> {
        val displays = getConnectedDisplaySizes(context, logger)
        if (displays.isNullOrEmpty()) {
            return emptyMap()
        }

        val primary = displays.firstOrNull { it.displayId == Display.DEFAULT_DISPLAY } ?: displays.first()

        val params = mutableMapOf<String, String>()
        params[Screens.PrimaryWidth.paramName] = "${primary.width}"
        params[Screens.PrimaryHeight.paramName] = "${primary.height}"
        params[Screens.PrimaryResolution.paramName] = resolutionString(primary)
        params[Screens.AllWidth.paramName] = displays.joinToString(";") { "${it.width}" }
        params[Screens.AllHeight.paramName] = displays.joinToString(";") { "${it.height}" }
        params[Screens.AllResolution.paramName] = displays.joinToString(";") { resolutionString(it) }
        params[Screens.AllCount.paramName] = "${displays.size}"
        return params
    }

    private fun resolutionString(display: DisplaySize): String = "${display.width},${display.height}"

    companion object {
        internal const val CACHE_LIFETIME_MILLIS = 3600_000L
    }
}
