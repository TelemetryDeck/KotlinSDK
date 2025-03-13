package com.telemetrydeck.sdk.params

internal enum class UserPreferences(val paramName: String) {
    LayoutDirection("TelemetryDeck.UserPreference.layoutDirection"),
    Region("TelemetryDeck.UserPreference.region"),
    Language("TelemetryDeck.UserPreference.language"),
    ColorScheme("TelemetryDeck.UserPreference.colorScheme"),
}