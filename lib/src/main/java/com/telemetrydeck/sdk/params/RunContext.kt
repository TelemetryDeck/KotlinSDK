package com.telemetrydeck.sdk.params


//"TelemetryDeck.RunContext.isDebug": "\(Self.isDebug)",
//"TelemetryDeck.RunContext.isSimulator": "\(Self.isSimulator)",
//"TelemetryDeck.RunContext.isTestFlight": "\(Self.isTestFlight)",
//"TelemetryDeck.RunContext.language": Self.appLanguage,
//"TelemetryDeck.RunContext.targetEnvironment": Self.targetEnvironment,

internal enum class RunContext(val paramName: String) {
    Locale("TelemetryDeck.RunContext.locale"),
    TargetEnvironment("TelemetryDeck.RunContext.targetEnvironment"),
    IsSideLoaded("TelemetryDeck.RunContext.isSideLoaded"),
    SourceMarketPlace("TelemetryDeck.RunContext.sourceMarketplace"),
}