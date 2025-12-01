package com.telemetrydeck.sdk.params

enum class ErrorCategory(val rawValue: String) {
    ThrownException("thrown-exception"),
    UserInput("user-input"),
    AppState("app-state")
}
