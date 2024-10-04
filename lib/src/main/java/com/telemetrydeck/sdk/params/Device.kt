package com.telemetrydeck.sdk.params


// TODO: add more device parameters from the Swift SDK:
//"TelemetryDeck.Device.orientation": Self.orientation,
//"TelemetryDeck.Device.screenResolutionHeight": Self.screenResolutionHeight,
//"TelemetryDeck.Device.screenResolutionWidth": Self.screenResolutionWidth,

internal enum class Device(val paramName: String) {
    Architecture("TelemetryDeck.Device.architecture"),
    ModelName("TelemetryDeck.Device.modelName"),
    OperatingSystem("TelemetryDeck.Device.operatingSystem"),
    Platform("TelemetryDeck.Device.platform"),
    SystemMajorMinorVersion("TelemetryDeck.Device.systemMajorMinorVersion"),
    SystemMajorVersion("TelemetryDeck.Device.systemMajorVersion"),
    SystemVersion("TelemetryDeck.Device.systemVersion"),
    Brand("TelemetryDeck.Device.brand"),
    TimeZone("TelemetryDeck.Device.timeZone"),
    Orientation("TelemetryDeck.Device.orientation"),
    ScreenDensity("TelemetryDeck.Device.screenDensity"),
    ScreenHeight("TelemetryDeck.Device.screenResolutionHeight"),
    ScreenWidth("TelemetryDeck.Device.screenResolutionWidth")
}