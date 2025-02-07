package com.telemetrydeck.sdk.providers

import android.content.Context
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.params.Device
import com.telemetrydeck.sdk.params.RunContext
import com.telemetrydeck.sdk.platform.getAppInstallationInfo
import com.telemetrydeck.sdk.platform.getDeviceOrientation
import com.telemetrydeck.sdk.platform.getDisplayMetrics
import com.telemetrydeck.sdk.platform.getLocaleName
import com.telemetrydeck.sdk.platform.getTimeZone
import java.lang.ref.WeakReference

internal class PlatformContextProvider : TelemetryDeckProvider {
    private var enabled: Boolean = true
    private var manager: WeakReference<TelemetryDeckSignalProcessor>? = null
    private var appContext: WeakReference<Context?>? = null
    private var metadata = mutableMapOf<String, String>()

    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        this.manager = WeakReference(client)
        this.appContext = WeakReference(ctx)

        if (ctx == null) {
            this.manager?.get()?.debugLogger?.error("RunContextProvider requires a context but received null. Signals will contain incomplete metadata.")
            this.enabled = false
            return
        }

        if (android.os.Build.DEVICE != null) {
            metadata[RunContext.TargetEnvironment.paramName] = android.os.Build.DEVICE
        }

        // determine if the app was installed by a trusted marketplace
        val appInfo = getAppInstallationInfo(ctx, this.manager?.get()?.debugLogger)
        if (appInfo != null) {
            metadata[RunContext.IsSideLoaded.paramName] = "${appInfo.isSideLoaded}"
            if (appInfo.sourceMarketPlace != null) {
                metadata[RunContext.SourceMarketPlace.paramName] = "${appInfo.sourceMarketPlace}"
            }
        }


        this.enabled = true
    }

    override fun stop() {
        this.enabled = false
    }

    override fun enrich(
        signalType: String,
        clientUser: String?,
        additionalPayload: Map<String, String>
    ): Map<String, String> {
        val signalPayload = additionalPayload.toMutableMap()
        for (item in metadata) {
            if (!signalPayload.containsKey(item.key)) {
                signalPayload[item.key] = item.value
            }
        }

        for (item in getDynamicAttributes()) {
            if (!signalPayload.containsKey(item.key)) {
                signalPayload[item.key] = item.value
            }
        }
        return signalPayload
    }

    // TODO: Use onConfigurationChanged instead

    private fun getDynamicAttributes(): Map<String, String> {
        val ctx = this.appContext?.get()
            ?: // can't read without a context!
            return emptyMap()

        val attributes = mutableMapOf<String, String>()

        // get current orientation
        val deviceOrientation = getDeviceOrientation(ctx, this.manager?.get()?.debugLogger)
        if (deviceOrientation != null) {
            attributes[Device.Orientation.paramName] = deviceOrientation.orientationName
        }

        // get current display metrics
        val displayMetrics = getDisplayMetrics(ctx, this.manager?.get()?.debugLogger)
        if (displayMetrics != null) {
            attributes[Device.ScreenWidth.paramName] = "${displayMetrics.width}"
            attributes[Device.ScreenHeight.paramName] = "${displayMetrics.height}"
            attributes[Device.ScreenDensity.paramName] = "${displayMetrics.density}"
        }

        // read the default locale
        val localeName: String? = getLocaleName(ctx, this.manager?.get()?.debugLogger)
        if (localeName != null) {
            attributes[RunContext.Locale.paramName] = localeName
        }

        // determine the current time zone
        val timeZoneInfo = getTimeZone(ctx, this.manager?.get()?.debugLogger)
        if (timeZoneInfo != null) {
            attributes[Device.TimeZone.paramName] = timeZoneInfo.id
        }

        return attributes
    }


}