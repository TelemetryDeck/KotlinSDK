package com.telemetrydeck.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import java.net.URL
import java.util.*

internal class ManifestMetadataReader {
    companion object {

        fun getConfigurationFromManifest(context: Context): TelemetryManagerConfiguration? {
            val bundle = getMetaData(context)
            return getConfigurationFromManifest(bundle)
        }

        fun getAppVersion(context: Context): String? {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                return packageInfo.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            return null
        }

        private fun getMetaData(context: Context): Bundle {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            return appInfo.metaData
        }

        private fun getConfigurationFromManifest(bundle: Bundle): TelemetryManagerConfiguration? {
            val appID = bundle.getString(ManifestSettings.AppID.key) ?: return null
            val config = TelemetryManagerConfiguration(appID)

            if (bundle.containsKey(ManifestSettings.ShowDebugLogs.key)) {
                config.showDebugLogs = bundle.getBoolean(ManifestSettings.ShowDebugLogs.key)
            }

            val apiBaseUrl = bundle.getString(ManifestSettings.ApiBaseURL.key)
            if (apiBaseUrl != null) {
                config.apiBaseURL = URL(apiBaseUrl)
            }

            if (bundle.containsKey(ManifestSettings.SendNewSessionBeganSignal.key)) {
                config.sendNewSessionBeganSignal = bundle.getBoolean(ManifestSettings.SendNewSessionBeganSignal.key)
            }

            val sessionID = bundle.getString(ManifestSettings.SessionID.key)
            if (sessionID != null) {
                config.sessionID = UUID.fromString(sessionID)
            }

            if (bundle.containsKey(ManifestSettings.TestMode.key)) {
                config.testMode = bundle.getBoolean(ManifestSettings.TestMode.key)
            }

            val defaultUser = bundle.getString(ManifestSettings.DefaultUser.key)
            if(defaultUser != null) {
                config.defaultUser = defaultUser
            }

            return config
        }
    }
}

