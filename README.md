# TelemetryDeck SDK

This package allows you to send signals to [TelemetryDeck](https://telemetrydeck.com) from your Android application. Sign up for a free account at telemetrydeck.com

## Installation

// TODO: Select a repository for hosting the library e.g. jitpack.io, maven central,...

Make sure your app is using SDK 28 or later

Add the following to your app's `build.gradle`:

```

// TODO: replace with published package reference
implementation project(':lib')

```

### Using the application manifest 

The TelemetryManager can be initialized automatically by adding the application key to the `application` section of the app's `AndroidManifest.xml`:


```
<application>
...

<meta-data android:name="com.telemetrydeck.sdk.appID" android:value="XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX" />

</application>
```

In addition, the following optional properties are supported:

* `com.telemetrydeck.sdk.showDebugLogs`

* `com.telemetrydeck.sdk.apiBaseURL`

* `com.telemetrydeck.sdk.sendNewSessionBeganSignal`

* `com.telemetrydeck.sdk.sessionID`

* `com.telemetrydeck.sdk.testMode`

* `com.telemetrydeck.sdk.defaultUser`



### Initialize programmatically

For greater control you can manually start the TelemetryManager client

```
val builder = TelemetryManager.Builder()
            .appID("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX")
            .showDebugLogs(true)
            .defaultUser("Person")

TelemetryManager.start(application, builder)

```


## Sending Signals

To send a signal immediately

```

TelemetryManager.send("appLaunchedRegularly")

```


To enqueue a signal to be sent by TelemetryManager at a later time

```
TelemetryManager.queue("appLaunchedRegularly")

```



## Requirements

* SDK 28 or later

* Kotlin 1.6.10 or later

* Java Compatibility Version 1.8
