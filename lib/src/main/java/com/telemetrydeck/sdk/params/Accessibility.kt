package com.telemetrydeck.sdk.params

enum class Accessibility(val paramName: String) {
    FontWeightAdjustment("TelemetryDeck.Accessibility.fontWeightAdjustment"),
    FontScale("TelemetryDeck.Accessibility.fontScale"),
    IsBoldTextEnabled("TelemetryDeck.Accessibility.isBoldTextEnabled"),
    IsDarkerSystemColorsEnabled("TelemetryDeck.Accessibility.isDarkerSystemColorsEnabled"),
    IsInvertColorsEnabled("TelemetryDeck.Accessibility.isInvertColorsEnabled"),
    IsReduceMotionEnabled("TelemetryDeck.Accessibility.isReduceMotionEnabled"),
    IsReduceTransparencyEnabled("TelemetryDeck.Accessibility.isReduceTransparencyEnabled"),
    ShouldDifferentiateWithoutColor("TelemetryDeck.Accessibility.shouldDifferentiateWithoutColor"),
}