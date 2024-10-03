package com.telemetrydeck.sdk.providers

import android.app.Application
import android.icu.util.VersionInfo
import com.telemetrydeck.sdk.BuildConfig
import com.telemetrydeck.sdk.ManifestMetadataReader
import com.telemetrydeck.sdk.TelemetryDeckClient
import com.telemetrydeck.sdk.TelemetryDeckProvider
import java.lang.ref.WeakReference
import java.util.Locale

/**
 * This provider enriches outgoing signals with additional parameters describing the current environment.
 *
 * - information about the specific app build, such as version, build number, or SDKs compiled with.
 * - information about the device running the application, such as operating system, model name, or architecture.
 * - information about the TelemetryDeck SDK, such as its name or version number.
 */
class EnvironmentParameterProvider  : TelemetryDeckProvider {
    private var enabled: Boolean = true
    private var manager: WeakReference<TelemetryDeckClient>? = null
    private var metadata = mutableMapOf<String, String>()

    override fun register(ctx: Application?, client: TelemetryDeckClient) {
        this.manager = WeakReference(client)

        if (ctx != null) {
            val appVersion = ManifestMetadataReader.getAppVersion(ctx)
            if (!appVersion.isNullOrEmpty()) {
                metadata["appVersion"] = appVersion
            }
            ManifestMetadataReader.getBuildNumber(ctx)?.let { buildNumber ->
                metadata["buildNumber"] = buildNumber.toString()
            }
        } else {
            this.manager?.get()?.debugLogger?.error("EnvironmentParameterProvider requires a context but received null. Signals will contain incomplete metadata.")
        }

        if (android.os.Build.VERSION.RELEASE.isNullOrEmpty()) {
            this.manager?.get()?.debugLogger?.error(
                "EnvironmentMetadataProvider found no platform version information (android.os.Build.VERSION.RELEASE). Signal payloads will not be enriched."
            )
        } else {
            val release = android.os.Build.VERSION.RELEASE
            val sdkVersion = android.os.Build.VERSION.SDK_INT
            metadata["systemVersion"] = "Android SDK: $sdkVersion ($release)"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val versionInfo = VersionInfo.getInstance(release)
                metadata["majorSystemVersion"] = versionInfo.major.toString()
                metadata["majorMinorSystemVersion"] = "${versionInfo.major}.${versionInfo.minor}"
            } else {
                val versionInfo = release.split(".")
                metadata["majorSystemVersion"] = versionInfo.elementAtOrNull(0) ?: "0"
                metadata["majorMinorSystemVersion"] = "${versionInfo.elementAtOrNull(0) ?: "0"}.${versionInfo.elementAtOrNull(1) ?: "0"}"
            }
        }

        metadata["locale"] = Locale.getDefault().displayName
        if (android.os.Build.BRAND != null) {
            metadata["brand"] = android.os.Build.BRAND
        }
        if (android.os.Build.DEVICE != null) {
            metadata["targetEnvironment"] = android.os.Build.DEVICE
        }
        if (android.os.Build.MODEL != null && android.os.Build.PRODUCT != null) {
            metadata["modelName"] = "${android.os.Build.MODEL} (${android.os.Build.PRODUCT})"
        }
        metadata["architecture"] = System.getProperty("os.arch") ?: ""
        metadata["operatingSystem"] = "Android"
        metadata["telemetryClientVersion"] = BuildConfig.LIBRARY_PACKAGE_NAME
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