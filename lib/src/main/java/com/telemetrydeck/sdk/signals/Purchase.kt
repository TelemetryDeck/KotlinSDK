package com.telemetrydeck.sdk.signals

enum class Purchase(val signalName: String) {
    Completed("TelemetryDeck.Purchase.completed"),
    FreeTrialStarted("TelemetryDeck.Purchase.freeTrialStarted"),
    ConvertedFromTrial("TelemetryDeck.Purchase.convertedFromTrial"),
}