package com.telemetrydeck.sdk

enum class SignalType(val type: String) {
    ActivityCreated("ActivityCreated"),
    ActivityStarted("ActivityStarted"),
    ActivityResumed("ActivityResumed"),
    ActivityPaused("onActivityPaused"),
    ActivityStopped("ActivityStopped"),
    ActivitySaveInstanceState("ActivitySaveInstanceState"),
    ActivityDestroyed("ActivityDestroyed"),
    ConfigurationChanged("ConfigurationChanged"),
    LowMemory("LowMemory"),
    AppBackgrounded("AppBackgrounded"),
}