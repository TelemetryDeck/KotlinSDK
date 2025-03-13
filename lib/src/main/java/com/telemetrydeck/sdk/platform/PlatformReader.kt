package com.telemetrydeck.sdk.platform

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.telemetrydeck.sdk.DebugLogger
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


internal fun getAppInstallationInfo(context: Context, logger: DebugLogger?): AppInstallationInfo? {
    try {
        val packageInfo = getPackageInfo(context, logger)
            ?: // we can't obtain further details without package information
            return null

        @Suppress("DEPRECATION")
        val sideLoaded =
            context.packageManager.getInstallerPackageName(packageInfo.packageName) == null
        return AppInstallationInfo(
            packageName = packageInfo.packageName,
            isSideLoaded = sideLoaded,
            sourceMarketPlace = null
        )
    } catch (e: Exception) {
        logger?.error("getAppInstallationInfo failed: $e ${e.stackTraceToString()}")
        return null
    }
}

internal fun getPackageInfo(context: Context, logger: DebugLogger?): PackageInfo? {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context
                .packageManager
                .getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            return context.packageManager.getPackageInfo(context.packageName, 0)
        }
    } catch (e: Exception) {
        logger?.error("getPackageInfo failed: $e ${e.stackTraceToString()}")
        return null
    }
}

internal fun getTimeZone(context: Context, logger: DebugLogger?): TimeZone? {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!context.resources.configuration.locales.isEmpty) {
                val locale = context.resources.configuration.locales[0]
                return Calendar.getInstance(locale).getTimeZone()
            }
        }
        return Calendar.getInstance().getTimeZone()
    } catch (e: Exception) {
        logger?.error("getTimeZone failed: $e ${e.stackTraceToString()}")
        return null
    }

}

internal fun getDeviceOrientation(context: Context, logger: DebugLogger?): DeviceOrientation? {
    try {
        val orientation = context.resources.configuration.orientation
        return when (orientation) {
            android.content.res.Configuration.ORIENTATION_LANDSCAPE -> DeviceOrientation.Landscape
            android.content.res.Configuration.ORIENTATION_PORTRAIT -> DeviceOrientation.Portrait
            @Suppress("DEPRECATION")
            android.content.res.Configuration.ORIENTATION_SQUARE -> DeviceOrientation.Square

            else -> DeviceOrientation.Unknown
        }
    } catch (e: Exception) {
        logger?.error("getDeviceOrientation failed: $e ${e.stackTraceToString()}")
        return null
    }
}

internal fun getDisplayMetrics(context: Context, logger: DebugLogger?): ScreenMetrics? {
    try {
        val metrics = context.resources.displayMetrics
        return ScreenMetrics(
            width = metrics.widthPixels,
            height = metrics.heightPixels,
            density = metrics.densityDpi
        )
    } catch (e: Exception) {
        logger?.error("getDisplayMetrics failed: $e ${e.stackTraceToString()}")
        return null
    }
}

internal fun getCurrentLocale(context: Context, logger: DebugLogger?): Locale? {
    try {
        val currentLocale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }

        return currentLocale
    } catch (e: Exception) {
        logger?.error("getLocale failed: $e ${e.stackTraceToString()}")
        return null
    }
}

internal fun getLocaleName(context: Context, logger: DebugLogger?): String? {
    return getCurrentLocale(context, logger)?.displayName
}