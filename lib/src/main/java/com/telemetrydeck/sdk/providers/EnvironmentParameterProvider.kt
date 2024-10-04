package com.telemetrydeck.sdk.providers

import android.app.Application
import android.icu.util.VersionInfo
import com.telemetrydeck.sdk.BuildConfig
import com.telemetrydeck.sdk.ManifestMetadataReader
import com.telemetrydeck.sdk.TelemetryDeckClient
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.params.AppInfo
import com.telemetrydeck.sdk.params.Device
import com.telemetrydeck.sdk.params.SDK
import com.telemetrydeck.sdk.platform.getTimeZone
import java.lang.ref.WeakReference

/**
 * This provider enriches outgoing signals with additional parameters describing the current environment.
 *
 * - information about the specific app build, such as version, build number, or SDKs compiled with.
 * - information about the device running the application, such as operating system, model name, or architecture.
 * - information about the TelemetryDeck SDK, such as its name or version number.
 */
class EnvironmentParameterProvider : TelemetryDeckProvider {
    private var enabled: Boolean = true
    private var manager: WeakReference<TelemetryDeckClient>? = null
    private var metadata = mutableMapOf<String, String>()

    // The approach from the SwiftSDK is not compatible here as we need to evaluate for platform capabilities
    // In case of Kotlin Multiplatform, a per-platform value can be provided
    // For now, we're defaulting to "Android"
    private val platform: String = "Android"
    private val os: String = "Android"
    private val sdkName: String = "KotlinSDK"

    override fun register(ctx: Application?, client: TelemetryDeckClient) {
        this.manager = WeakReference(client)

        if (ctx != null) {
            val appVersion = ManifestMetadataReader.getAppVersion(ctx)
            if (!appVersion.isNullOrEmpty()) {
                metadata[AppInfo.Version.paramName] = appVersion
            }
            ManifestMetadataReader.getBuildNumber(ctx)?.let { buildNumber ->
                metadata[AppInfo.BuildNumber.paramName] = buildNumber.toString()
                metadata[AppInfo.VersionAndBuildNumber.paramName] =
                    "$appVersion (build $buildNumber)"
            }

            // determine the current time zone
            val timeZoneInfo = getTimeZone(ctx.applicationContext, this.manager?.get()?.debugLogger)
            if (timeZoneInfo != null) {
                metadata[Device.TimeZone.paramName] = timeZoneInfo.displayName
            }

        } else {
            this.manager?.get()?.debugLogger?.error("EnvironmentParameterProvider requires a context but received null. Signals will contain incomplete metadata.")
        }



        if (android.os.Build.VERSION.RELEASE.isNullOrEmpty()) {
            this.manager?.get()?.debugLogger?.error(
                "EnvironmentMetadataProvider found no platform version information (android.os.Build.VERSION.RELEASE). Signal payloads will not be enriched."
            )
        } else {
            // Device metadata
            metadata[Device.Platform.paramName] = platform
            val release = android.os.Build.VERSION.RELEASE
            val sdkVersion = android.os.Build.VERSION.SDK_INT
            metadata[Device.SystemVersion.paramName] = "$platform $release (SDK: $sdkVersion)"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
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

        if (android.os.Build.BRAND != null) {
            metadata[Device.Brand.paramName] = android.os.Build.BRAND
        }
        if (android.os.Build.MODEL != null && android.os.Build.PRODUCT != null) {
            metadata[Device.ModelName.paramName] =
                "${android.os.Build.MODEL} (${android.os.Build.PRODUCT})"
        }
        metadata[Device.Architecture.paramName] = System.getProperty("os.arch") ?: ""
        metadata[Device.OperatingSystem.paramName] = os


        // SDK Metadata
        metadata[SDK.Name.paramName] = sdkName

        metadata[SDK.Version.paramName] = BuildConfig.LIBRARY_PACKAGE_NAME
        metadata[SDK.NameAndVersion.paramName] = "$sdkName ${BuildConfig.LIBRARY_PACKAGE_NAME}"
        metadata[SDK.BuildType.paramName] = BuildConfig.BUILD_TYPE

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