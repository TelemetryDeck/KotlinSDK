package com.telemetrydeck.sdk.signals

internal enum class Activation(val signalName: String) {
    OnboardingCompleted("TelemetryDeck.Activation.onboardingCompleted"),
    CoreFeatureUsed("TelemetryDeck.Activation.coreFeatureUsed"),
}