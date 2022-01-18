package com.telemetrydeck.sdk

import android.app.Application
import android.icu.util.VersionInfo
import android.util.Log
import java.util.*

/**
 * Adds environment and device information to outgoing Signals.
 */
class EnvironmentMetadataProvider : TelemetryProvider {
    private val tag: String = "TELEMETRYDECK"
    private var enabled: Boolean = true
    private var metadata = mutableMapOf<String, String>()

    init {
        if (android.os.Build.VERSION.RELEASE.isNullOrEmpty()) {
            Log.e(
                tag,
                "EnvironmentMetadataProvider found no platform version information (android.os.Build.VERSION.RELEASE). Signal payloads will not be enriched."
            )
        } else {
            val release = android.os.Build.VERSION.RELEASE
            val sdkVersion = android.os.Build.VERSION.SDK_INT
            metadata["systemVersion"] = "Android SDK: $sdkVersion ($release)"

            val versionInfo = VersionInfo.getInstance(release)
            metadata["majorSystemVersion"] = versionInfo.major.toString()
            metadata["majorMinorSystemVersion"] = "${versionInfo.major}.${versionInfo.minor}"
        }

        metadata["locale"] = Locale.getDefault().displayName
        metadata["brand"] = android.os.Build.BRAND
        metadata["targetEnvironment"] = android.os.Build.DEVICE
        metadata["architecture"] = System.getProperty("os.arch") ?: ""
        metadata["modelName"] = "${android.os.Build.MODEL} (${android.os.Build.PRODUCT})"
        metadata["operatingSystem"] = "Android"
        metadata["telemetryClientVersion"] = BuildConfig.LIBRARY_PACKAGE_NAME
    }

    override fun register(ctx: Application?, manager: TelemetryManager) {
        if (ctx == null) {
            manager.logger?.error("EnvironmentMetadataProvider requires a context but received null. Signals will contain incomplete metadata.")
        }
        metadata["appVersion"] = ManifestMetadataReader.getAppVersion(ctx!!) ?: ""
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
            signalPayload[item.key] = item.value
        }
        return signalPayload
    }
}