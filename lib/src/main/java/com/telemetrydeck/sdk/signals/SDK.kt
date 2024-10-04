package com.telemetrydeck.sdk.signals

internal enum class SDK(val signalName: String) {
    Name("TelemetryDeck.SDK.name"),
    Version("TelemetryDeck.SDK.version"),
    NameAndVersion("TelemetryDeck.SDK.nameAndVersion"),
}