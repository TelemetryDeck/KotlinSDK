package com.telemetrydeck.sdk.providers

import android.content.Context
import android.icu.util.VersionInfo
import android.os.Build
import com.telemetrydeck.sdk.BuildConfig
import com.telemetrydeck.sdk.DebugLogger
import com.telemetrydeck.sdk.ManifestMetadataReader
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.params.AppInfo
import com.telemetrydeck.sdk.params.Device
import com.telemetrydeck.sdk.params.SDK

/**
 * This provider enriches outgoing signals with additional parameters describing the current environment.
 *
 * - information about the specific app build, such as version, build number, or SDKs compiled with.
 * - information about the device running the application, such as operating system, model name, or architecture.
 * - information about the TelemetryDeck SDK, such as its name or version number.
 */
class EnvironmentParameterProvider : TelemetryDeckProvider {
    private var enabled: Boolean = true
    private var metadata = mutableMapOf<String, String>()

    // The approach from the SwiftSDK is not compatible here as we need to evaluate for platform capabilities
    // In case of Kotlin Multiplatform, a per-platform value can be provided
    // For now, we're defaulting to "Android"
    private val platform: String = "Android"
    private val os: String = "Android"
    private val sdkName: String = "KotlinSDK"
    private val sdkVersion: String = "4.1.0"

    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        appendContextSpecificParams(ctx, client.debugLogger)
        appendVersionMetadata(client.debugLogger)
        appendBrandAndMakeMetadata()
        appendSDKMetadata()
        this.enabled = true
    }

    private fun appendSDKMetadata() {
        metadata[SDK.Name.paramName] = sdkName
        metadata[SDK.Version.paramName] = sdkVersion
        metadata[SDK.NameAndVersion.paramName] = "$sdkName $sdkVersion"
        metadata[SDK.BuildType.paramName] = BuildConfig.BUILD_TYPE
    }

    private fun appendBrandAndMakeMetadata() {
        metadata[Device.Platform.paramName] = platform

        if (Build.BRAND != null) {
            metadata[Device.Brand.paramName] = Build.BRAND
        }
        if (Build.MODEL != null && Build.PRODUCT != null) {
            metadata[Device.ModelName.paramName] =
                "${Build.MODEL} (${Build.PRODUCT})"
        }
        metadata[Device.Architecture.paramName] = System.getProperty("os.arch") ?: ""
        metadata[Device.OperatingSystem.paramName] = os
    }

    private fun appendVersionMetadata(debugLogger: DebugLogger?) {
        if (Build.VERSION.RELEASE.isNullOrEmpty()) {
            debugLogger?.error(
                "android.os.Build.VERSION.RELEASE is not set - device metadata will not be appended"
            )
            return
        }


        val release = Build.VERSION.RELEASE
        val sdkVersion = Build.VERSION.SDK_INT
        metadata[Device.SystemVersion.paramName] = "$platform $release (SDK: $sdkVersion)"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val versionInfo = VersionInfo.getInstance(release)
            metadata[Device.SystemMajorVersion.paramName] = "${versionInfo.major}"
            metadata[Device.SystemMajorMinorVersion.paramName] =
                "${versionInfo.major}.${versionInfo.minor}"
        } else {
            val versionInfo = release.split(".")
            val major = versionInfo.elementAtOrNull(0) ?: "0"
            val minor = versionInfo.elementAtOrNull(1) ?: "0"
            metadata[Device.SystemMajorVersion.paramName] = major
            metadata[Device.SystemMajorMinorVersion.paramName] = "$major.$minor"
        }
    }

    private fun appendContextSpecificParams(ctx: Context?, debugLogger: DebugLogger?) {
        if (ctx == null) {
            debugLogger?.error("EnvironmentParameterProvider requires a context but received null. Signals will contain incomplete metadata.")
            return
        }

        val appVersion = ManifestMetadataReader.getAppVersion(ctx)
        if (!appVersion.isNullOrEmpty()) {
            metadata[AppInfo.Version.paramName] = appVersion
        }
        ManifestMetadataReader.getBuildNumber(ctx)?.let { buildNumber ->
            metadata[AppInfo.BuildNumber.paramName] = buildNumber.toString()
            metadata[AppInfo.VersionAndBuildNumber.paramName] =
                "$appVersion (build $buildNumber)"
        }
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