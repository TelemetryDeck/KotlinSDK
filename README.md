# Kotlin SDK for TelemetryDeck

This package allows you to send signals to [TelemetryDeck](https://telemetrydeck.com) from your
Android applications. Sign up for a free account at [telemetrydeck.com](https://telemetrydeck.com)

* [Installation](#installation)
    * [Dependencies](#dependencies)
    * [Permission for internet access](#permission-for-internet-access)
* [Getting Started](#getting-started)
    * [Using the application manifest](#using-the-application-manifest)
    * [Programmatic Usage](#programmatic-usage)
* [Sending Signals](#sending-signals)
* [User Identifiers](#user-identifiers)
    * [Custom User Identifiers](#custom-user-identifiers)
    * [Environment Parameters](#environment-parameters)
* [Default Parameters](#default-parameters)
* [Default Prefix](#default-prefix)
* [Navigation Signals](#navigation-signals)
* [Acquisition](#acquisition)
* [Session Tracking](#session-tracking)
* [Custom Telemetry](#custom-telemetry)
* [Purchase Completed](#purchase-completed)
* [Custom Logging](#custom-logging)
* [Requirements](#requirements)
* [Migrating providers to 5.0+](#migrating-providers-to-50)
* [Migrating providers to 3.0+](#migrating-providers-to-30)

## Installation

### Dependencies

The Kotlin SDK for TelemetryDeck is available from Maven Central at the following coordinates:

```groovy
// `build.gradle`
dependencies {
    implementation 'com.telemetrydeck:kotlin-sdk:6.2.1'
}
```

```kotlin
// `build.gradle.kts`
dependencies {
    implementation("com.telemetrydeck:kotlin-sdk:6.2.1")
}
```

If needed, update your `gradle.settings` to reference Kotlin version compatible with 2.0.20, e.g.:

```
id "org.jetbrains.kotlin.android" version "2.0.20" apply false
```

### Permission for internet access

Sending signals requires access to the internet so the following permission should be added to the
app's `AndroidManifest.xml`

```xml

<uses-permission android:name="android.permission.INTERNET" />
```

## Getting Started

### Using the application manifest

A quick way to start is by adding your App ID to the `application` section of the app's
`AndroidManifest.xml`:

```xml

<application>
    ...

    <meta-data android:name="com.telemetrydeck.appID"
        android:value="XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX" />

</application>
```

Replace `XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX` with your TelemetryDeck App ID.

In addition, the following optional properties are supported:

- `com.telemetrydeck.showDebugLogs`
- `com.telemetrydeck.apiBaseURL`
- `com.telemetrydeck.sendNewSessionBeganSignal`
- `com.telemetrydeck.testMode`
- `com.telemetrydeck.defaultUser`

### Programmatic Usage

For greater control you can set up and configure `TelemetryDeck` using the provided builder:

```kotlin
val builder = TelemetryDeck.Builder()
    .appID("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
    .showDebugLogs(true)
    .defaultUser("Person")

TelemetryDeck.start(applicationContext, builder)
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

## User Identifiers

When `TelemetryDeck` is started for the first time, it will create a user identifier for the user
that is specific to the app installation.

- The identity is stored within the application's file folder on the user's device.

- The identifier will be removed when a user uninstalls an app. The KotlinSDK will not "bridge" the
  user's identity between installations.

- Users can reset the identifier at any time by using the "Clear Data" action in Settings of their
  device.

If you have a better user identifier available, such as an email address or a username, you can use
that instead, by setting `defaultUser` (the identifier will be hashed before sending it) in
configuration, or by passing the value when sending signals.

### Custom User Identifiers

If you need a more robust mechanism for keep track of the user's identity, you can replace the
default behaviour by providing your own implementation of `TelemetryDeckIdentityProvider`:

```kotlin
val builder = TelemetryDeck.Builder()
    .appID("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
    .showDebugLogs(true)
    .defaultUser("Person")
    .identityProvider(YourIdentityProvider())

TelemetryDeck.start(applicationContext, builder)
```

### Environment Parameters

By default, Kotlin SDK for TelemetryDeck will include the following environment parameters for each
outgoing signal

| Parameter name                                                | Provider                        | Description                                        |
|---------------------------------------------------------------|---------------------------------|----------------------------------------------------|
| `TelemetryDeck.AppInfo.buildNumber`                           | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.AppInfo.version`                               | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.AppInfo.versionAndBuildNumber`                 | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.Device.architecture`                           | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.Device.modelName`                              | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.Device.operatingSystem`                        | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.Device.platform`                               | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.Device.systemMajorMinorVersion`                | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.Device.systemMajorVersion`                     | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.Device.systemVersion`                          | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.Device.orientation`                            | `PlatformContextProvider`       |                                                    |
| `TelemetryDeck.Device.screenDensity`                          | `PlatformContextProvider`       |                                                    |
| `TelemetryDeck.Device.screenResolutionHeight`                 | `PlatformContextProvider`       |                                                    |
| `TelemetryDeck.Device.screenResolutionWidth`                  | `PlatformContextProvider`       |                                                    |
| `TelemetryDeck.Device.brand`                                  | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.Device.timeZone`                               | `PlatformContextProvider`       |                                                    |
| `TelemetryDeck.AppInfo.buildNumber`                           | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.AppInfo.version`                               | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.AppInfo.versionAndBuildNumber`                 | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.SDK.name`                                      | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.SDK.version`                                   | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.SDK.nameAndVersion`                            | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.SDK.buildType`                                 | `EnvironmentParameterProvider`  |                                                    |
| `TelemetryDeck.RunContext.locale`                             | `PlatformContextProvider`       |                                                    |
| `TelemetryDeck.RunContext.targetEnvironment`                  | `PlatformContextProvider`       |                                                    |
| `TelemetryDeck.RunContext.isSideLoaded`                       | `PlatformContextProvider`       |                                                    |
| `TelemetryDeck.RunContext.sourceMarketplace`                  | `PlatformContextProvider`       |                                                    |
| `TelemetryDeck.Accessibility.isBoldTextEnabled`               | `AccessibilityProvider`         | API 31 and above                                   |
| `TelemetryDeck.Accessibility.fontWeightAdjustment`            | `AccessibilityProvider`         | API 31 and above                                   |
| `TelemetryDeck.Accessibility.isDarkerSystemColorsEnabled`     | `AccessibilityProvider`         |                                                    |
| `TelemetryDeck.Accessibility.fontScale`                       | `AccessibilityProvider`         | Mapped to iOS size categories                      |
| `TelemetryDeck.Accessibility.isInvertColorsEnabled`           | `AccessibilityProvider`         |                                                    |
| `TelemetryDeck.Accessibility.isReduceMotionEnabled`           | `AccessibilityProvider`         |                                                    |
| `TelemetryDeck.Accessibility.isReduceTransparencyEnabled`     | `AccessibilityProvider`         |                                                    |
| `TelemetryDeck.Accessibility.shouldDifferentiateWithoutColor` | `AccessibilityProvider`         |                                                    |
| `TelemetryDeck.UserPreference.layoutDirection`                | `AccessibilityProvider`         | Possible values are "rightToLeft" or "leftToRight" |
| `TelemetryDeck.UserPreference.region`                         | `AccessibilityProvider`         | Current device region in ISO 3166-1 alpha-2 format |
| `TelemetryDeck.UserPreference.language`                       | `AccessibilityProvider`         | Current application language in ISO 639-1 format   |
| `TelemetryDeck.UserPreference.colorScheme`                    | `AccessibilityProvider`         | "Dark" or "Light"                                  |
| `TelemetryDeck.Session.started`                               | `SessionTrackingSignalProvider` |                                                    |
| `TelemetryDeck.Acquisition.newInstallDetected`                | `SessionTrackingSignalProvider` |                                                    |
| `TelemetryDeck.Acquisition.firstSessionDate`                  | `SessionTrackingSignalProvider` |                                                    |
| `TelemetryDeck.Retention.averageSessionSeconds`               | `SessionTrackingSignalProvider` |                                                    |
| `TelemetryDeck.Retention.distinctDaysUsed`                    | `SessionTrackingSignalProvider` |                                                    |
| `TelemetryDeck.Retention.totalSessionsCount`                  | `SessionTrackingSignalProvider` |                                                    |
| `TelemetryDeck.Retention.previousSessionSeconds`              | `SessionTrackingSignalProvider` |                                                    |
| `TelemetryDeck.Retention.distinctDaysUsedLastMonth`           | `SessionTrackingSignalProvider` |                                                    |
| `TelemetryDeck.Calendar.dayOfMonth`                           | `CalendarParameterProvider`     |                                                    |
| `TelemetryDeck.Calendar.dayOfWeek`                            | `CalendarParameterProvider`     |                                                    |
| `TelemetryDeck.Calendar.dayOfYear`                            | `CalendarParameterProvider`     |                                                    |
| `TelemetryDeck.Calendar.weekOfYear`                           | `CalendarParameterProvider`     |                                                    |
| `TelemetryDeck.Calendar.isWeekend`                            | `CalendarParameterProvider`     |                                                    |
| `TelemetryDeck.Calendar.monthOfYear`                          | `CalendarParameterProvider`     |                                                    |
| `TelemetryDeck.Calendar.quarterOfYear`                        | `CalendarParameterProvider`     |                                                    |
| `TelemetryDeck.Calendar.hourOfDay`                            | `CalendarParameterProvider`     |                                                    |

#### Notes

- `TelemetryDeck.Acquisition.newInstallDetected`

We send this signal when a user starts the app for the first time on a given device.

- Session data is stored locally on device as part of the application's files.
- If the application is uninstalled or it's data cleared, the SDK will report a new installation
  event (we do not bridge session data of any kind between installations).

See [Session Tracking](#session-tracking) on how sessions are tracked.

- `TelemetryDeck.Accessibility.fontScale` - the value is mapped to better align with size categories
  sent by other SDKs:

```
fontScale <= 0.8f ->                      XS
fontScale > 0.8f && fontScale < 0.9f ->   S
fontScale >= 0.9f && fontScale < 1.0f ->  M
fontScale == 1.0f ->                      L
fontScale >= 1.0f && fontScale < 1.3f ->  XL
fontScale >= 1.3f && fontScale < 1.4f ->  XXL
fontScale >= 1.4f && fontScale < 1.5f ->  XXXL
fontScale >= 1.5f && fontScale < 1.6f ->  AccessibilityM
fontScale >= 1.6f && fontScale < 1.7f ->  AccessibilityL
fontScale >= 1.7f && fontScale < 1.8f ->  AccessibilityXL
fontScale >= 1.8f && fontScale < 1.9f ->  AccessibilityXXL
fontScale >= 1.9f && fontScale < 2.0f ->  AccessibilityXXXL
fontScale >= 2.0f ->                      AccessibilityXXXL
```

See [Custom Telemetry](#custom-telemetry) on how to implement your own parameter enrichment.

## Default Parameters

If there are parameters you would like to include with every outgoing signal, you can use
`DefaultParameterProvider` instead of passing them with every call.

```kotlin
// create an instance of [DefaultParameterProvider] and pass the key value you wish to be appended to every signal
val provider = DefaultParameterProvider(mapOf("key" to "value"))

// add the provider when configuring an instance of TelemetryDeck

val builder = TelemetryDeck.Builder()
    .appID("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
    .addProvider(provider)
```

## Default Prefix

If you find yourself prepending the same prefix for to your custom signals or parameters,
you can optionally configure `TelemetryDeck` to do this for you by activating our
`DefaultPrefixProvider`:

```kotlin
// create an instance of [DefaultPrefixProvider] with a signal or parameter prefix
val provider = DefaultPrefixProvider("MyApp.", "MyApp.Params.")

// add the provider when configuring an instance of TelemetryDeck

val builder = TelemetryDeck.Builder()
    .appID("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
    .addProvider(provider)
```

## Navigation Signals

You can make use
of [Navigation Signals](https://telemetrydeck.com/docs/articles/navigation-signals/) to better
understand how your users a moving through the app.

```kotlin
// track a navigation event e.g. when the user is moving from one screen to another:
TelemetryDeck.navigate(sourcePath = "/onboarding", destinationPath = "/home")

// let TelemetryDeck take care of tracking the user's route by calling navigate when the path changes
TelemetryDeck.navigate("/onboarding")
TelemetryDeck.navigate("/home")
```

## Acquisition

The following helper methods are available

```kotlin
/**
 * Send a `TelemetryDeck.Acquisition.userAcquired` signal with the provided channel.
 */
fun acquiredUser(channel: String, ...)
```

```kotlin
/**
 * Send a `TelemetryDeck.Acquisition.leadStarted` signal with the provided leadId.
 */
fun leadStarted(leadId: String, ...)
```

```kotlin
/**
 * Send a `TelemetryDeck.Acquisition.leadConverted` signal with the provided leadId.
 */
fun leadConverted(leadId: String, ...)
```

## Session Tracking

The SDK uses session tracking detect when the app is launched for the first time (
`TelemetryDeck.Acquisition.newInstallDetected`) and enrich signals with default parameters regarding
user retention like session duration, days used, number of sessions etc.

- Session state is stored within the application's file folder on the user's device.

- The session state will be removed when a user uninstalls an app. The SDK does not "bridge" state
  between installations.

- Users can reset the session state at any time by using the "Clear Data" action in Settings of
  their device.

The SDK tracks sessions by means of a session manager. The default session manager is
`SessionTrackingSignalProvider` and it is enabled by default.

Here are some concepts on which the `SessionTrackingSignalProvider` is based:

### Foreground/Active Time

This refers to the time during which the app is actively being used by the user.
It's the period when the app is in the foreground and interacting with the user.

### Starting a session

A session typically begins when the user opens the app or resumes interaction after a period of
inactivity.

Note: If `sendNewSessionBeganSignal` is set to `true`, the `TelemetryDeck.Session.started` is send
for every start of a new session.

### Completed Session

A completed session is defined as the time between two subsequent session starts.
Essentially, it's the duration from when the app becomes active until it becomes active again after
a period of inactivity or closure.

**Example:**

- First Session Start
  The user opens the app at 10:00. A new session is started.
  The user actively uses the app until 10:15, then minimizes it or switches to another app.

- Second Session Start
  The user returns to the app at 10:30. A new session is started again.
  The user uses the app until 10:45, then closes it.

- Third Session Start
  The user opens the app again at 11:00. A new session is started for the third time.
  The user uses the app until 11:10.

**Results**

| Completed Session | Start Time | End Time | Duration |
|-------------------|------------|----------|----------|
| First             | 10:00      | 10:30    | 30 min   |
| Second            | 10:30      | 11:00    | 30 min   |

The third session will not be counted until the next time the user opens the app.

### Custom sessionID

In some situations, you may want to control the session identifier.

Session IDs are UUIDs which the SDK would generate automatically but you can provide your own as
follows:

* Retrieve the current session ID using `TelemetryDeck.sessionID`. If the value is `null`, the
  session manager is disabled or no session has been started yet.

* Start/end a session on demand by calling `TelemetryDeck.newSession()` and optionally passing a
  custom sessionID.

* Instruct the SDK to start with a custom sessionID using the builder:

```kotlin
val builder = TelemetryDeck.Builder()
    .appID("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
    .sessionID(UUID.fromString("00000000-0000-0000-0000-000000000000"))
```

* Session tracking is optional and can be deactivated by disabling the session manager:

```kotlin
val builder = TelemetryDeck.Builder()
    .appID("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
    .sessionManager(null)
```

* You can provide your own logic for session tracking by adopting
  `TelemetryDeckSessionManagerProvider` and setting it as the session manager.

## Duration Signals

The SDK offers convenience methods to facilitate tracking the duration of specific objects or events.

Once started, a duration signal will be tracked internally by the SDK and upon completion, it will send the signal while also adding a `TelemetryDeck.Signal.durationInSeconds` parameter.

```kotlin
// start tracking, without sending a signal
TelemetryDeck.startDurationSignal("wizard_step1")

// end tracking, sends the signal including the total duration (excluding background time) 
TelemetryDeck.stopAndSendDurationSignal("wizard_step1")
```

Duration signals are provided by the `DurationSignalTrackerProvider` which is always enabled. 


## Calendar Parameters

By default, the KotlinSDK will append the following parameters to all outgoing signals:

- `TelemetryDeck.Calendar.dayOfMonth`: The day-of-month (1..31) component of the date.
- `TelemetryDeck.Calendar.dayOfWeek`: The ISO 8601 number of the given day of the week. Monday is 1, Sunday is 7.
- `TelemetryDeck.Calendar.dayOfYear`: The 1-based day-of-year component of the date.
- `TelemetryDeck.Calendar.weekOfYear`: The week number within the current year as defined by `getFirstDayOfWeek()` and `getMinimalDaysInFirstWeek()`.
- `TelemetryDeck.Calendar.isWeekend`: `true` if the day of the week is Saturday or Sunday, `false` otherwise.
- `TelemetryDeck.Calendar.monthOfYear`: The number-of-the-month (1..12) component of the date.
- `TelemetryDeck.Calendar.quarterOfYear`: The the quarter-of-year (1..4). For API 26 and earlier, it's the number of the month divided by 3.
- `TelemetryDeck.Calendar.hourOfDay`: The hour-of-day (0..23) time component of this time value.


## Purchase Completed

The SDK offers a facility method `TelemetryDeck.purchaseCompleted()` that can be invoked in response to a user purchase.
With information from the marketplace where the user made a purchase, you can inform TelemetryDeck as follows:

```kotlin
TelemetryDeck.purchaseCompleted(
            event = PurchaseEvent.PAID_PURCHASE,
            countryCode = "BE",
            productID = "product1",
            purchaseType = PurchaseType.ONE_TIME_PURCHASE,
            priceAmountMicros = 7990000,
            currencyCode = "EUR",
            offerID = "offer1"
        )
```

Depending on the specified `PurchaseEvent`, one of the following signals will be sent:
- `TelemetryDeck.Purchase.completed`
- `TelemetryDeck.Purchase.freeTrialStarted`
- `TelemetryDeck.Purchase.convertedFromTrial`


The `floatValue` of the signal will be set to the provided purchase amount, converted `USD`.

The following parameters are also included with the signal:

| Parameter                             | Description                                                                   |
|---------------------------------------|-------------------------------------------------------------------------------|
| `TelemetryDeck.Purchase.type`         | `subscription` or `one-time-purchase`                                         |
| `TelemetryDeck.Purchase.countryCode`  | The country code of the marketplace where the purchase was made               |
| `TelemetryDeck.Purchase.currencyCode` | The ISO 4217 currency code (e.g., "EUR" for Euro) used for the purchase.      |
| `TelemetryDeck.Purchase.priceMicros`  | The price of the product in micro-units of the currency                       |
| `TelemetryDeck.Purchase.productID`    | The unique identifier of the purchased product                                |
| `TelemetryDeck.Purchase.offerID`      | The specific offer identifier for subscription products or customized pricing |


### Google Play

When integrating with Google Play Billing Library, you can adopt the TelemetryDeck SDK with Google Play Services in order to let us determine the exact purchase parameters.

1. Add the following package as a dependency to your app:

```kotlin
// `build.gradle.kts`
dependencies {
    implementation("com.telemetrydeck:kotlin-sdk-google-services:6.2.1")
}
```

2. You can now use the `purchaseCompleted` function optimized for Google Play Billing:

```kotlin
fun purchaseHandlerInYourApp(
  billingConfig: BillingConfig,
  purchase: Purchase,
  productDetails: ProductDetails
) {
  TelemetryDeck.purchaseCompleted(
    billingConfig = billingConfig,
    purchase = purchase,
    productDetails = productDetails
  )
}
```

By default, this method assumes the purchase is a `PAID_PURCHASE`. To determine if the user is converting or starting a trial, you will have to implement your own [server-side validation](https://developer.android.com/google/play/billing/integrate) and inspect the `paymentState` of a subscription.

To make it easier to get started, the TelemetryDeck SDK offers a helper method which attempts to guess the purchase origin based on locally available data, so you could:

```kotlin
TelemetryDeck.purchaseCompleted(
  billingConfig = billingConfig,
  purchase = purchase,
  productDetails = productDetails,
  purchaseOrigin = purchase.toTelemetryDeckPurchaseEvent(setOf("TRIAL_SKU"))
)
```

Note that this approach is not exact and comes with a certain number of limitations, please check the doc notes on `com.telemetrydeck.sdk.googleservices.toTelemetryDeckPurchaseEvent` for more details.


## Custom Telemetry

Another way to send signals is to implement a custom `TelemetryDeckProvider`.
A provider uses the TelemetryDeck client in order to queue or send signals based on environment or
other triggers.

To create a provider, implement the `TelemetryDeckProvider` interface:

```kotlin
class CustomProvider : TelemetryDeckProvider {
    override fun register(ctx: Context?, client: TelemetryDeckClient) {
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
- You can use `WeakReference<TelemetryDeckClient>` if you need to be able to call the TelemetryDeck
  at a later time.

To use your custom provider, register it by calling `addProvider` using the
`TelemetryDeck.Builder` :

```kotlin
val builder = TelemetryDeck.Builder()
    //    ...
    .addProvider(CustomProvider()) // <-- Your custom provider
```

Every time the SDK is about to send signals to our servers, the `enrich` method of every provider
will be invoked to give you the opportunity to append additional parameters.

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

- `SessionAppProvider` - Emits signals for application and activity lifecycle events. This provider
  is tasked with resetting the sessionID when `sendNewSessionBeganSignal` is enabled.
- `SessionActivityProvider` - Emits signals for application and activity lifecycle events. This
  provider is not enabled by default.
- `EnvironmentParameterProvider` - Adds environment and device information to outgoing Signals. This
  provider overrides the `enrich` method in order to append additional metadata for all signals
  before sending them.
- `PlatformContextProvider` - Adds environment and device information which may change over time
  like the current timezone and screen metrics.
- `AccessibilityProvider` - Adds parameters describing the currently active accessibility options.
- `SessionTrackingSignalProvider` - Reports when a new app installation has been detected.

For a complete list, check the `com.telemetrydeck.sdk.providers` package.

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

By default, TelemetryDeck SDK uses a simple `println` to output internal diagnostic messages when
`showDebugLogs` is set to `true` in configuration.

If your platform has custom logging needs, you can adopt the `DebugLogger` interface and provide it
to the `TelemetryDeck` builder:

```kotlin
val builder = TelemetryDeck.Builder()
    //    ...
    .logger(CustomLogger())
```

Please note that the logger implementation should be thread safe as it may be invoked in different
queues and contexts.

## Requirements

- Android API 21 or later
- Kotlin 2.0.20
- Gradle 6.8.3–8.8\*
- AGP 7.1.3–8.5

## Migrating providers to 5.0+

* The provider interface `TelemetryDeckProvider` has changed to accept a `Context` instance instead
  of an `Application`.
* The deprecated fallback provider callbacks are no longer used and the functionality has been
  removed.
* Providers can now optionally override the `transform` method in order to modify any component of
  the signal.

## Migrating providers to 3.0+

If you had Kotlin SDK for TelemetryDeck added to your app, you will notice that `TelemetryManager`
and related classes have been deprecated.
You can read more about the motivation behind these
changes [here](https://telemetrydeck.com/docs/articles/grand-rename/).

To upgrade, please perform the following changes depending on how you use TelemetryDeck SDK.

### If you're using the application manifest

- Adapt the manifest of your app and rename all keys from `com.telemetrydeck.sdk.*` to
  `com.telemetrydeck.*` for example:

Before:

```xml

<meta-data android:name="com.telemetrydeck.sdk.appID"
    android:value="XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX" />
```

After:

```xml

<meta-data android:name="com.telemetrydeck.appID"
    android:value="XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX" />
```

- In your app sourcecode, rename all uses of `TelemetryManager` to `TelemetryDeck`.
- If you were using `send()` to send signals, no further changes are needed!
- If you were using `queue()` to send signals, you will need to rename the method to
  `TelemetryDeck.signal()`.

### Programmatic Usage

- In your app sourcecode, rename all uses of `TelemetryManager` to `TelemetryDeck`.
- If you were using `send()` to send signals, no further changes are needed!
- If you were using `queue()` to send signals, you will need to rename the method to
  `TelemetryDeck.signal()`.
- If you had a custom provider configuration, please replace the corresponding providers as follows:

| Provider (old name)             | Provider (new, 3.0+)                                      |
|---------------------------------|-----------------------------------------------------------|
| `AppLifecycleTelemetryProvider` | `SessionAppProvider`, `SessionActivityProvider`           |
| `SessionProvider`               | `SessionAppProvider`                                      |
| `EnvironmentMetadataProvider`   | `EnvironmentParameterProvider`, `PlatformContextProvider` |

> [!TIP]
> You can rename all deprecated classes in your project using the Code Cleanup function in
> IntelliJ/Android Studio.

> [!WARNING]
> Do not mix usage of `TelemetryManager` and `TelemetryDeck`. Once you're ready to migrate, adapt
> all uses at the same time.

### Custom Telemetry

Your custom providers must replace `TelemetryProvider` with `TelemetryDeckProvider`.

To adopt the new interface:

- Adapt the signature of the `register` method to
  `register(ctx: Context?, client: TelemetryDeckSignalProcessor)`

The `TelemetryDeckSignalProcessor` interface offers a subset of the `TelemetryDeck` client API which
gives you access to:

- To access the logger, use can use `client.debugLogger`
- To access the signal cache, use `client.signalCache`
