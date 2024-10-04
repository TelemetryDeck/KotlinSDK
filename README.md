# KotlinClient

This package allows you to send signals to [TelemetryDeck](https://telemetrydeck.com) from your Android applications. Sign up for a free account at [telemetrydeck.com](https://telemetrydeck.com)

## Installation

### Dependencies

The TelemetryDeck SDK for Kotlin is available from Maven Central and can be used as a dependency directly in `build.gradle` file:

```groovy
dependencies {
    // ...
    // Please replace 1.0.0 with the latest version of the SDK
    implementation 'com.github.TelemetryDeck:KotlinSDK:1.0.0'
}
```

If needed, update your `gradle.settings` to reference Kotlin version compatible with 1.9.25, e.g.:

```
id "org.jetbrains.kotlin.android" version "1.9.25" apply false
```

### Permission for internet access

Sending signals requires access to the internet so the following permission should be added to the app's `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Getting Started

### Using the application manifest

The TelemetryDeck can be initialized automatically by adding the application key to the `application` section of the app's `AndroidManifest.xml`:

```xml
<application>
...

<meta-data android:name="com.telemetrydeck.appID" android:value="XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX" />

</application>
```

Replace `XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX` with your TelemetryDeck App ID.

In addition, the following optional properties are supported:

- `com.telemetrydeck.showDebugLogs`
- `com.telemetrydeck.apiBaseURL`
- `com.telemetrydeck.sendNewSessionBeganSignal`
- `com.telemetrydeck.sessionID`
- `com.telemetrydeck.testMode`
- `com.telemetrydeck.defaultUser`

### Programmatic Usage

For greater control you can instead manually start the TelemetryDeck client

```kotlin
val builder = TelemetryDeck.Builder()
            .appID("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
            .showDebugLogs(true)
            .defaultUser("Person")

TelemetryDeck.start(application, builder)
```

## Sending Signals

To send a signal immediately

```kotlin
TelemetryDeck.send("appLaunchedRegularly")
```

To enqueue a signal to be sent by TelemetryDeck at a later time

```kotlin
TelemetryDeck.signal("appLaunchedRegularly")
```

### Environment Parameters

By default, TelemetryDeck will include the following environment parameters for each outgoing signal


| Signal name                                    | Provider                       |
|------------------------------------------------|--------------------------------|
| `TelemetryDeck.Session.started`                | `SessionAppProvider`           |
| `TelemetryDeck.AppInfo.buildNumber`            | `EnvironmentParameterProvider` |
| `TelemetryDeck.AppInfo.version`                | `EnvironmentParameterProvider` |
| `TelemetryDeck.AppInfo.versionAndBuildNumber`  | `EnvironmentParameterProvider` |
| `TelemetryDeck.Device.architecture`            | `EnvironmentParameterProvider` |
| `TelemetryDeck.Device.modelName`               | `EnvironmentParameterProvider` |
| `TelemetryDeck.Device.operatingSystem`         | `EnvironmentParameterProvider` |
| `TelemetryDeck.Device.platform`                | `EnvironmentParameterProvider` |
| `TelemetryDeck.Device.systemMajorMinorVersion` | `EnvironmentParameterProvider` |
| `TelemetryDeck.Device.systemMajorVersion`      | `EnvironmentParameterProvider` |
| `TelemetryDeck.Device.systemVersion`           | `EnvironmentParameterProvider` |
| `TelemetryDeck.Device.orientation`             | `PlatformContextProvider`      |
| `TelemetryDeck.Device.screenDensity`           | `PlatformContextProvider`      |
| `TelemetryDeck.Device.screenResolutionHeight`  | `PlatformContextProvider`      |
| `TelemetryDeck.Device.screenResolutionWidth`   | `PlatformContextProvider`      |
| `TelemetryDeck.Device.brand`                   | `EnvironmentParameterProvider` |
| `TelemetryDeck.Device.timeZone`                | `EnvironmentParameterProvider` |
| `TelemetryDeck.AppInfo.buildNumber`            | `EnvironmentParameterProvider` |
| `TelemetryDeck.AppInfo.version`                | `EnvironmentParameterProvider` |
| `TelemetryDeck.AppInfo.versionAndBuildNumber`  | `EnvironmentParameterProvider` |
| `TelemetryDeck.SDK.name`                       | `EnvironmentParameterProvider` |
| `TelemetryDeck.SDK.version`                    | `EnvironmentParameterProvider` |
| `TelemetryDeck.SDK.nameAndVersion`             | `EnvironmentParameterProvider` |
| `TelemetryDeck.RunContext.locale`              | `PlatformContextProvider`      |
| `TelemetryDeck.RunContext.targetEnvironment`   | `PlatformContextProvider`      |
| `TelemetryDeck.RunContext.isSideLoaded`        | `PlatformContextProvider`      |
| `TelemetryDeck.RunContext.sourceMarketplace`   | `PlatformContextProvider`      |


See [Custom Telemetry](#custom-telemetry) on how to implement your own parameter enrichment.

## Custom Telemetry

Another way to send signals is to register a custom `TelemetryDeckProvider`.
A provider uses the TelemetryDeck client in order to queue or send signals based on environment or other triggers.


To create a provider, implement the `TelemetryDeckProvider` interface:

```kotlin
class CustomProvider: TelemetryDeckProvider {
    override fun register(ctx: Application?, client: TelemetryDeckClient) {
        // configure and start the provider
        // you may retain a WeakReference to client 
    }

    override fun stop() {
        // deactivate the provider, perform cleanup work
    }
}
```

Setup and start the provider during the `register` method.

Tips:

- Do not retain a strong reference to the application context or TelemetryDeckClient instance.
- You can use `WeakReference<TelemetryDeckClient>` if you need to be able to call the TelemetryDeck at a later time.

To use your custom provider, register it by calling `addProvider` using the `TelemetryDeck.Builder` :

```kotlin
val builder = TelemetryDeck.Builder()
            //    ...
            .addProvider(CustomProvider()) // <-- Your custom provider
```

Every time the SDK is about to send signals to our servers, the `enrich` method of every provider will be invoked to give you the opportunity to append additional parameters.

In the implementation of your custom `TelemetryDeckProvider`, you can override the `enrich` method:

```kotlin
override fun enrich(
        signalType: String,
        clientUser: String?,
        additionalPayload: Map<String, String>
    ): Map<String, String> {
        // retrieve the payload of signal
        val signalPayload = additionalPayload.toMutableMap()
        // add additional attributes of your choice
        val today = LocalDateTime.now().dayOfWeek
        if (today == DayOfWeek.MONDAY) {
            signalPayload["isMonday"] = "yes"
        }
        // return the enriched payload
        return signalPayload
    }
```

We use providers internally to provide lifecycle and environment integration out of the box.
Feel free to examine how they work and inspire your own implementations.

You can also completely disable or override the default providers with your own.

- `SessionAppProvider` - Emits signals for application and activity lifecycle events. This provider is tasked with resetting the sessionID when `sendNewSessionBeganSignal` is enabled.
- `SessionActivityProvider` - Emits signals for application and activity lifecycle events. This provider is not enabled by default.
- `EnvironmentParameterProvider` - Adds environment and device information to outgoing Signals. This provider overrides the `enrich` method in order to append additional metadata for all signals before sending them.
- `PlatformContextProvider` - Adds environment and device information which may change over time like the current timezone and screen metrics.

```kotlin
// Append a custom provider
val builder = TelemetryDeck.Builder()
           //    ...
           .addProvider(CustomProvider())


// Replace all default providers
val builder = TelemetryDeck.Builder()
            //    ...
            .providers(listOf(CustomProvider(), AnotherProvider()))
```

## Custom Logging

By default, TelemetryDeck SDK uses a simple `println` to output internal diagnostic messages when `showDebugLogs` is set to `true` in configuration.

If your platform has custom logging needs, you can adopt the `DebugLogger` interface and provide it to the `TelemetryDeck` builder:

```kotlin
val builder = TelemetryDeck.Builder()
           //    ...
           .logger(CustomLogger())
```

Please note that the logger implementation should be thread safe as it may be invoked in different queues and contexts. 



## Requirements

- Android API 21 or later
- Kotlin 1.9.25 or later


## Migrating providers to 3.0+

If you had TelemetryDeck SDK for Kotlin added to your app, you will notice that `TelemetryManager` and related classes have been deprecated.
You can read more about the motivation behind these changes [here](https://telemetrydeck.com/docs/articles/grand-rename/).

To upgrade, please perform the following changes depending on how you use TelemetryDeck SDK.

### If you're using the application manifest

* Adapt the manifest of your app and rename all keys from `com.telemetrydeck.sdk.*` to `com.telemetrydeck.*` for example:

Before:
```xml
<meta-data android:name="com.telemetrydeck.sdk.appID" android:value="XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX" />
```

After:
```xml
<meta-data android:name="com.telemetrydeck.appID" android:value="XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX" />
```

* In your app sourcecode, rename all uses of `TelemetryManager` to `TelemetryDeck`.
* If you were using `send()` to send signals, no further changes are needed!
* If you were using `queue()` to send signals, you will need to rename the method to `TelemetryDeck.signal()`.

### Programmatic Usage

* In your app sourcecode, rename all uses of `TelemetryManager` to `TelemetryDeck`.
* If you were using `send()` to send signals, no further changes are needed!
* If you were using `queue()` to send signals, you will need to rename the method to `TelemetryDeck.signal()`.
* If you had a custom provider configuration, please replace the corresponding providers as follows:

| Provider (old name)             | Provider (new, 3.0+)                                      |
|---------------------------------|-----------------------------------------------------------|
| `AppLifecycleTelemetryProvider` | `SessionAppProvider`, `SessionActivityProvider`           |
| `SessionProvider`               | `SessionAppProvider`                                      |
| `EnvironmentMetadataProvider`   | `EnvironmentParameterProvider`, `PlatformContextProvider` |


> [!TIP]
> You can rename all deprecated classes in your project using the Code Cleanup function in IntelliJ/Android Studio.


### Custom Telemetry


Your custom providers must replace `TelemetryProvider` with `TelemetryDeckProvider`.

To adopt the new interface:

* Adapt the signature of the `register` method to `register(ctx: Application?, client: TelemetryDeckClient)`

You now have access to the entire `TelemetryDeckClient` interface:

* To access the logger, use can use `client.debugLogger`
* To access the signal cache, use `client.signalCache`


