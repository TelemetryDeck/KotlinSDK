package com.telemetrydeck.sdk

internal enum class ManifestSettings(val key: String) {
    AppID("com.telemetrydeck.sdk.appID"),
    ShowDebugLogs("com.telemetrydeck.sdk.showDebugLogs"),
    ApiBaseURL("com.telemetrydeck.sdk.apiBaseURL"),
    SendNewSessionBeganSignal("com.telemetrydeck.sdk.sendNewSessionBeganSignal"),
    SessionID("com.telemetrydeck.sdk.sessionID"),
    TestMode("com.telemetrydeck.sdk.testMode"),
    DefaultUser("com.telemetrydeck.sdk.defaultUser"),
    Salt("com.telemetrydeck.sdk.salt"),
}