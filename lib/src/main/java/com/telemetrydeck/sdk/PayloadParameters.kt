package com.telemetrydeck.sdk

@Deprecated("PayloadParameters is no longer part of our public API. You're free to create a custom enum for custom signal names.")
enum class PayloadParameters(val type: String) {
    TelemetryDeckNavigationSchemaVersion("TelemetryDeck.Navigation.schemaVersion"),
    TelemetryDeckNavigationIdentifier("TelemetryDeck.Navigation.identifier"),
    TelemetryDeckNavigationSourcePath("TelemetryDeck.Navigation.sourcePath"),
    TelemetryDeckNavigationDestinationPath("TelemetryDeck.Navigation.destinationPath"),
}