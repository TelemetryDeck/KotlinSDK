package com.telemetrydeck.sdk.params

internal enum class Navigation(val paramName: String) {
    SchemaVersion("TelemetryDeck.Navigation.schemaVersion"),
    Identifier("TelemetryDeck.Navigation.identifier"),
    SourcePath("TelemetryDeck.Navigation.sourcePath"),
    DestinationPath("TelemetryDeck.Navigation.destinationPath"),
}