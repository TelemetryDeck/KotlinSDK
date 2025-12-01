package com.telemetrydeck.sdk.signals

enum class Activation(val signalName: String) {
    OnboardingCompleted("TelemetryDeck.Activation.onboardingCompleted"),
    CoreFeatureUsed("TelemetryDeck.Activation.coreFeatureUsed"),
}