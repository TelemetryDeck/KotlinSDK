package com.telemetrydeck.sdk

enum class PayloadParameters(val type: String) {
    TelemetryDeckNavigationSchemaVersion("TelemetryDeck.Navigation.schemaVersion"),
    TelemetryDeckNavigationIdentifier("TelemetryDeck.Navigation.identifier"),
    TelemetryDeckNavigationSourcePath("TelemetryDeck.Navigation.sourcePath"),
    TelemetryDeckNavigationDestinationPath("TelemetryDeck.Navigation.destinationPath"),
}