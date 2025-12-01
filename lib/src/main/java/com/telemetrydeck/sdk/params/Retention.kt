package com.telemetrydeck.sdk.params

enum class Retention(val paramName: String) {
    AverageSessionSeconds("TelemetryDeck.Retention.averageSessionSeconds"),
    DistinctDaysUsed("TelemetryDeck.Retention.distinctDaysUsed"),
    TotalSessionsCount("TelemetryDeck.Retention.totalSessionsCount"),
    PreviousSessionSeconds("TelemetryDeck.Retention.previousSessionSeconds"),
    DistinctDaysUsedLastMonth("TelemetryDeck.Retention.distinctDaysUsedLastMonth")
}