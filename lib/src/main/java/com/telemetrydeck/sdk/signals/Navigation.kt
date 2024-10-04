package com.telemetrydeck.sdk.signals

internal enum class Navigation(val signalName: String) {
    SchemaVersion("TelemetryDeck.Navigation.schemaVersion"),
    Identifier("TelemetryDeck.Navigation.identifier"),
    SourcePath("TelemetryDeck.Navigation.sourcePath"),
    DestinationPath("TelemetryDeck.Navigation.destinationPath"),
}