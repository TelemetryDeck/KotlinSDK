package com.telemetrydeck.sdk.signals

internal enum class AppInfo(val signalName: String) {
    BuildNumber("TelemetryDeck.AppInfo.buildNumber"),
    Version("TelemetryDeck.AppInfo.version"),
    VersionAndBuildNumber("TelemetryDeck.AppInfo.versionAndBuildNumber"),
}

