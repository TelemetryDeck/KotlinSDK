package com.telemetrydeck.sdk.signals

internal enum class Purchase(val signalName: String) {
    Completed("TelemetryDeck.Purchase.completed"),
    FreeTrialStarted("TelemetryDeck.Purchase.freeTrialStarted"),
    ConvertedFromTrial("TelemetryDeck.Purchase.convertedFromTrial"),
}