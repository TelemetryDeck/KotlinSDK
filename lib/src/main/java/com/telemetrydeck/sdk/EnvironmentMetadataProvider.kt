package com.telemetrydeck.sdk

import android.app.Application
import android.icu.util.VersionInfo
import java.util.*

/**
 * Adds environment and device information to outgoing Signals.
 */
class EnvironmentMetadataProvider : TelemetryProvider {
    private var enabled: Boolean = true
    private var metadata = mutableMapOf<String, String>()

    override fun register(ctx: Application?, manager: TelemetryManagerSignals) {
        if (ctx != null) {
            val appVersion = ManifestMetadataReader.getAppVersion(ctx)
            if (!appVersion.isNullOrEmpty()) {
                metadata["appVersion"] = appVersion
            }
            ManifestMetadataReader.getBuildNumber(ctx)?.let { buildNumber ->
                metadata["buildNumber"] = buildNumber.toString()
            }
        } else {
            manager.debugLogger?.error("EnvironmentMetadataProvider requires a context but received null. Signals will contain incomplete metadata.")
        }
        if (android.os.Build.VERSION.RELEASE.isNullOrEmpty()) {
            manager.debugLogger?.error(
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
