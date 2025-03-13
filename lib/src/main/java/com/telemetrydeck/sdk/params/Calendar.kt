package com.telemetrydeck.sdk.params

internal enum class Calendar(val paramName: String) {
    DayOfMonth("TelemetryDeck.Calendar.dayOfMonth"),
    DayOfWeek("TelemetryDeck.Calendar.dayOfWeek"),
    DayOfYear("TelemetryDeck.Calendar.dayOfYear"),
    WeekOfYear("TelemetryDeck.Calendar.weekOfYear"),
    IsWeekend("TelemetryDeck.Calendar.isWeekend"),
    MonthOfYear("TelemetryDeck.Calendar.monthOfYear"),
    QuarterOfYear("TelemetryDeck.Calendar.quarterOfYear"),
    HourOfDay("TelemetryDeck.Calendar.hourOfDay"),
}