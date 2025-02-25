package com.telemetrydeck.sdk

internal enum class TelemetryDeckManifestSettings(val key: String) {
    AppID("com.telemetrydeck.appID"),
    ShowDebugLogs("com.telemetrydeck.showDebugLogs"),
    ApiBaseURL("com.telemetrydeck.apiBaseURL"),
    SendNewSessionBeganSignal("com.telemetrydeck.sendNewSessionBeganSignal"),
    TestMode("com.telemetrydeck.testMode"),
    DefaultUser("com.telemetrydeck.defaultUser"),
    Salt("com.telemetrydeck.salt"),
}