package com.telemetrydeck.sdk.platform

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.util.Calendar
import java.util.TimeZone


internal fun getAppInstallationInfo(context: Context): AppInstallationInfo? {
    val packageInfo = getPackageInfo(context)
        ?: // we can't obtain further details without package information
        return null

    val sideLoaded = context.packageManager.getInstallerPackageName(packageInfo.packageName) == null
    return AppInstallationInfo(
        packageName = packageInfo.packageName,
        isSideLoaded = sideLoaded,
        sourceMarketPlace = null
    )
}

internal fun getPackageInfo(context: Context): PackageInfo? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return context
            .packageManager
            .getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0));
    } else {
        @Suppress("DEPRECATION")
        return context.packageManager.getPackageInfo(context.packageName, 0);
    }
}

internal fun getTimeZone(context: Context): TimeZone? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        if (!context.resources.configuration.locales.isEmpty) {
            val locale = context.resources.configuration.locales[0]
            return Calendar.getInstance(locale).getTimeZone()
        }
    }
    return Calendar.getInstance().getTimeZone()
}