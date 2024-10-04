package com.telemetrydeck.sdk.providers

import android.app.Application
import com.telemetrydeck.sdk.TelemetryDeckClient
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.platform.getAppInstallationInfo
import com.telemetrydeck.sdk.params.RunContext
import java.lang.ref.WeakReference
import java.util.Locale

class PlatformContextProvider: TelemetryDeckProvider {
    private var enabled: Boolean = true
    private var manager: WeakReference<TelemetryDeckClient>? = null
    private var metadata = mutableMapOf<String, String>()

    override fun register(ctx: Application?, client: TelemetryDeckClient) {
        this.manager = WeakReference(client)
        if (ctx == null) {
            this.manager?.get()?.debugLogger?.error("RunContextProvider requires a context but received null. Signals will contain incomplete metadata.")
            this.enabled = false
            return
        }

        if (android.os.Build.DEVICE != null) {
            metadata[RunContext.TargetEnvironment.paramName] = android.os.Build.DEVICE
        }

        // read the default locale
        metadata[RunContext.Locale.paramName] = Locale.getDefault().displayName

        // determine if the app was installed by a trusted marketplace
        val appInfo = getAppInstallationInfo(ctx)
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
        return signalPayload
    }
}