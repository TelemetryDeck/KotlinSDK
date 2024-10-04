package com.telemetrydeck.sdk

@Deprecated("SignalType is no longer part of our public API. You're free to create a custom enum for custom signal names.")
enum class SignalType(val type: String) {
    ActivityCreated("ActivityCreated"), ActivityStarted("ActivityStarted"), ActivityResumed("ActivityResumed"), ActivityPaused(
        "ActivityPaused"
    ),
    ActivityStopped("ActivityStopped"), ActivitySaveInstanceState("ActivitySaveInstanceState"), ActivityDestroyed(
        "ActivityDestroyed"
    ),
    AppBackground("AppBackground"),
    AppForeground("AppForeground"),
    NewSessionBegan("NewSessionBegan"),
    TelemetryDeckNavigationPathChanged(
        "TelemetryDeck.Navigation.pathChanged"
    )
}
