# KotlinClient

This package allows you to send signals to [TelemetryDeck](https://telemetrydeck.com) from your Android applications. Sign up for a free account at [telemetrydeck.com](https://telemetrydeck.com)

## Installation

The TelemetryDeck is distributed using [jitpack](https://jitpack.io/), so you'll need to add the jitpack dependency to your `settings.gradle` file:

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' } // <-- add this line
    }
}
```

After that is done, add the following to your `build.gradle` file, under `dependencies`:

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

## Permission for internet access

Sending signals requires access to the internet so the following permission should be added to the app's `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### Using the application manifest

The TelemetryDeck can be initialized automatically by adding the application key to the `application` section of the app's `AndroidManifest.xml`:

```xml
<application>
...

<meta-data android:name="com.telemetrydeck.sdk.appID" android:value="XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX" />

</application>
```

Replace `XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX` with your TelemetryDeck App ID.

In addition, the following optional properties are supported:

- `com.telemetrydeck.sdk.showDebugLogs`
- `com.telemetrydeck.sdk.apiBaseURL`
- `com.telemetrydeck.sdk.sendNewSessionBeganSignal`
- `com.telemetrydeck.sdk.sessionID`
- `com.telemetrydeck.sdk.testMode`
- `com.telemetrydeck.sdk.defaultUser`

### Programatic Usage

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
TelemetryDeck.queue("appLaunchedRegularly")
```

## Custom Telemetry

Another way to send signals is to register a custom `TelemetryProvider` . A provider maintains a reference to the TelemetryDeck in order to queue or send signals.

To create a provider, implement the `TelemetryProvider` interface:

```kotlin
class CustomProvider: TelemetryProvider {
    override fun register(ctx: Application?, manager: TelemetryDeck) {
        // configure and start the provider
    }

    override fun stop() {
        // deactivate the provider
    }
}
```

Setup and start the provider during the `register` method.

Tips:

- Do not retain a strong reference to the application context or the TelemetryDeck.
- You can use `WeakReference<TelemetryDeck>` if you need to be able to call the TelemetryDeck at a later time.

To use your custom provider, register it using the `TelemetryDeck.Builder` :

```kotlin
val builder = TelemetryDeck.Builder()
            .appID("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
            .addProvider(CustomProvider())
```

When a signal is received by TelemetryDeck, it can be enriched with platform and environment specific information. TelemetryDeck calls the `enrich` method allowing every registered provider to add additional payload to a signal.

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

TelemetryDeck also makes use of providers in order to provide lifecycle and environment integration out of the box. Feel free to examine how they work and inspire your own implementations. You can also completely disable or override the default providers with your own.

- `SessionProvider` - Monitors the app lifecycle in order to broadcast the NewSessionBegan signal. This provider is tasked with resetting the sessionID when `sendNewSessionBeganSignal` is enabled.
- `AppLifecycleTelemetryProvider` - Emits signals for application and activity lifecycle events.
- `EnvironmentMetadataProvider` - Adds environment and device information to outgoing Signals. This provider overrides the `enrich` method in order to append additional metdata for all signals before sending them.

```kotlin
// Append a custom provider
val builder = TelemetryDeck.Builder()
           .appID("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
           .addProvider(CustomProvider())


// Replace all default providers
val builder = TelemetryDeck.Builder()
            .appID("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
            .providers(listOf(CustomProvider(), AnotherProvider()))
```

## Requirements

- Android API 21 or later
- Kotlin 1.9.25 or later
