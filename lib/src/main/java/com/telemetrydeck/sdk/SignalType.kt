package com.telemetrydeck.sdk

enum class SignalType(val type: String) {
    ActivityCreated("ActivityCreated"),
    ActivityStarted("ActivityStarted"),
    ActivityResumed("ActivityResumed"),
    ActivityPaused("ActivityPaused"),
    ActivityStopped("ActivityStopped"),
    ActivitySaveInstanceState("ActivitySaveInstanceState"),
    ActivityDestroyed("ActivityDestroyed"),
    AppBackground("AppBackground"),
    AppForeground("AppForeground"),
    NewSessionBegan("NewSessionBegan"),
}
