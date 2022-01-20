# TelemetryDeck SDK

This package allows you to send signals to [TelemetryDeck](https://telemetrydeck.com) from your
Android application. Sign up for a free account at [telemetrydeck.com](http://telemetrydeck.com)

## Installation

// TODO: Select a repository for hosting the library e.g. [jitpack.io](http://jitpack.io), maven
central,...

Add the following to your app's `build.gradle`:

```groovy
// TODO: replace with published package reference
implementation project(':lib')
```

### Permission for internet access

Sending signals requires access to the internet so the following permission should be added to the
app's `AndroidManifest.xml`

```xml

<uses-permission android:name="android.permission.INTERNET" />
```

### Using the application manifest

The TelemetryManager can be initialized automatically by adding the application key to
the `application` section of the app's `AndroidManifest.xml`:

```xml

<application>
    ...

    <meta-data android:name="com.telemetrydeck.sdk.appID"
        android:value="XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX" />

</application>
```

In addition, the following optional properties are supported:

- `com.telemetrydeck.sdk.showDebugLogs`
- `com.telemetrydeck.sdk.apiBaseURL`
- `com.telemetrydeck.sdk.sendNewSessionBeganSignal`
- `com.telemetrydeck.sdk.sessionID`
- `com.telemetrydeck.sdk.testMode`
- `com.telemetrydeck.sdk.defaultUser`

### Programatically

For greater control you can manually start the TelemetryManager client

```kotlin
val builder = TelemetryManager.Builder()
    .appID("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
    .showDebugLogs(true)
    .defaultUser("Person")

TelemetryManager.start(application, builder)
```

## Sending Signals

To send a signal immediately

```kotlin
TelemetryManager.send("appLaunchedRegularly")
```

To enqueue a signal to be sent by TelemetryManager at a later time

```kotlin
TelemetryManager.queue("appLaunchedRegularly")
```

## Custom Telemetry

Another way to send signals is to register a custom `TelemetryProvider` . A provider maintains a
reference to the TelemetryManager in order to queue or send signals.

To create a provider, implement the `TelemetryProvider` interface:

```kotlin
class CustomProvider : TelemetryProvider {
    override fun register(ctx: Application?, manager: TelemetryManager) {
        //...
    }

    override fun stop() {
        //...
    }
}
```

Setup and start the provider during the `register` method.

NB: Do not retain references to the application context or the TelemetryManager.

To use a custom provider, register it using the `TelemetryManager.Builder` :

```kotlin
val builder = TelemetryManager.Builder()
    .appID("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
    .addProvider(CustomProvider())
```

When a signal is received by TelemetryManager, it can be enriched with platform and environment
specific information. TelemetryManager calls the `enrich` method allowing every registered provider
to add additional payload to a signal.

```kotlin
override fun enrich(
    signalType: String,
    clientUser: String?,
    additionalPayload: Map<String, String>
): Map<String, String> {
    val signalPayload = additionalPayload.toMutableMap()
    val today = LocalDateTime.now().dayOfWeek
    if (today == DayOfWeek.MONDAY) {
        signalPayload["isMonday"] = "yes"
    }
    return signalPayload
}
```

## Requirements

- SDK 28 or later
- Kotlin 1.6.10 or later
- Java Compatibility Version 1.8

