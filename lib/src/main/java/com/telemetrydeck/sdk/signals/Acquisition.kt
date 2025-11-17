package com.telemetrydeck.sdk.signals

enum class Acquisition(val signalName: String) {
    NewInstallDetected("TelemetryDeck.Acquisition.newInstallDetected"),
    LeadStarted("TelemetryDeck.Acquisition.leadStarted"),
    UserAcquired("TelemetryDeck.Acquisition.userAcquired"),
    LeadConverted("TelemetryDeck.Acquisition.leadConverted"),
}