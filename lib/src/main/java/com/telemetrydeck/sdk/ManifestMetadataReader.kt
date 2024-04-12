package com.telemetrydeck.sdk

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.pm.PackageInfoCompat
import java.net.URL
import java.util.*

internal class ManifestMetadataReader {
    companion object {

        fun getConfigurationFromManifest(context: Context): TelemetryManagerConfiguration? {
            val bundle = getMetaData(context)
            if (bundle != null) {
                return getConfigurationFromManifest(context, bundle)
            }
            return null
        }

        fun getAppVersion(context: Context): String? {
            return getPackageInfo(context)?.versionName
        }

        fun getBuildNumber(context: Context): Long? {
            return getPackageInfo(context)?.let { PackageInfoCompat.getLongVersionCode(it) }
        }

        private fun getPackageInfo(context: Context): PackageInfo? {
            return try {
                 context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                null
            }
        }

        private fun getMetaData(context: Context): Bundle? {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            return appInfo.metaData
        }

        /**
         * Creates an instance of TelemetryManagerConfiguration by reading the manifest.
         *
         */
        private fun getConfigurationFromManifest(context: Context, bundle: Bundle): TelemetryManagerConfiguration? {
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
            } else {
                config.testMode = 0 != (context.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_DEBUGGABLE
            }

            val defaultUser = bundle.getString(ManifestSettings.DefaultUser.key)
            if(defaultUser != null) {
                config.defaultUser = defaultUser
            }

            val salt = bundle.getString(ManifestSettings.Salt.key)
            if(salt != null) {
                config.salt = salt
            }

            return config
        }
    }
}
