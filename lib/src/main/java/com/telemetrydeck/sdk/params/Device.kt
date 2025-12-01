package com.telemetrydeck.sdk.params


enum class Device(val paramName: String) {
    Architecture("TelemetryDeck.Device.architecture"),
    ModelName("TelemetryDeck.Device.modelName"),
    OperatingSystem("TelemetryDeck.Device.operatingSystem"),
    Platform("TelemetryDeck.Device.platform"),
    SystemMajorMinorVersion("TelemetryDeck.Device.systemMajorMinorVersion"),
    SystemMajorVersion("TelemetryDeck.Device.systemMajorVersion"),
    SystemVersion("TelemetryDeck.Device.systemVersion"),
    Brand("TelemetryDeck.Device.brand"),
    TimeZone("TelemetryDeck.Device.timeZone"),
    Orientation("TelemetryDeck.Device.orientation"), // iOS compatibility note: on Android, there are additional orientations
    ScreenDensity("TelemetryDeck.Device.screenDensity"),
    ScreenHeight("TelemetryDeck.Device.screenResolutionHeight"),
    ScreenWidth("TelemetryDeck.Device.screenResolutionWidth"),
}