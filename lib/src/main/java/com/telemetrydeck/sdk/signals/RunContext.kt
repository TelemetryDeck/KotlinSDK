package com.telemetrydeck.sdk.signals


//"TelemetryDeck.RunContext.isAppStore": "\(Self.isAppStore)",
//"TelemetryDeck.RunContext.isDebug": "\(Self.isDebug)",
//"TelemetryDeck.RunContext.isSimulator": "\(Self.isSimulator)",
//"TelemetryDeck.RunContext.isTestFlight": "\(Self.isTestFlight)",
//"TelemetryDeck.RunContext.language": Self.appLanguage,
//"TelemetryDeck.RunContext.targetEnvironment": Self.targetEnvironment,

internal enum class RunContext(val signalName: String) {
    Locale("TelemetryDeck.RunContext.locale"),
    TargetEnvironment("TelemetryDeck.RunContext.targetEnvironment"),
}