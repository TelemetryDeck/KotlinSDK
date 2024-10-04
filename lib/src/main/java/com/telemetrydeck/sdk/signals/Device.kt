package com.telemetrydeck.sdk.signals


// TODO: add more device parameters from the Swift SDK:
//"TelemetryDeck.Device.orientation": Self.orientation,
//"TelemetryDeck.Device.screenResolutionHeight": Self.screenResolutionHeight,
//"TelemetryDeck.Device.screenResolutionWidth": Self.screenResolutionWidth,
//"TelemetryDeck.Device.timeZone": Self.timeZone,

internal enum class Device(val signalName: String) {
    Architecture("TelemetryDeck.Device.architecture"),
    ModelName("TelemetryDeck.Device.modelName"),
    OperatingSystem("TelemetryDeck.Device.operatingSystem"),
    Platform("TelemetryDeck.Device.platform"),
    SystemMajorMinorVersion("TelemetryDeck.Device.systemMajorMinorVersion"),
    SystemMajorVersion("TelemetryDeck.Device.systemMajorVersion"),
    SystemVersion("TelemetryDeck.Device.systemVersion"),
    Brand("TelemetryDeck.Device.brand"),
}