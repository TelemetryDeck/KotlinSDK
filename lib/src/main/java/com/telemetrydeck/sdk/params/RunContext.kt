package com.telemetrydeck.sdk.params


// The following are not provided by the Kotlin SDK:
// TelemetryDeck.RunContext.isDebug
// TelemetryDeck.RunContext.isSimulator
// TelemetryDeck.RunContext.isTestFlight
// TelemetryDeck.RunContext.language
// TelemetryDeck.UserPreference.language
// TelemetryDeck.UserPreference.region

enum class RunContext(val paramName: String) {
    Locale("TelemetryDeck.RunContext.locale"),
    TargetEnvironment("TelemetryDeck.RunContext.targetEnvironment"),
    IsSideLoaded("TelemetryDeck.RunContext.isSideLoaded"),
    SourceMarketPlace("TelemetryDeck.RunContext.sourceMarketplace"),
}