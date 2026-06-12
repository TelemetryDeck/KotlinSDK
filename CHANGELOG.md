# Changelog

## 7.0.0

### Before You Upgrade

**Version 7.0.0 contains breaking changes.** Please read the migration notes below before updating.

### Breaking Changes

#### Minimum Android API level is now 23 (Android 6.0)

The SDK no longer supports Android 5.x (API 21 and 22). If your app still targets those API levels, stay on 6.x until you raise your own `minSdk`.

#### Android Gradle Plugin 9 and Gradle 9 required

The SDK is now built with AGP 9.2.1 and Gradle 9.5. If your project has not yet migrated to AGP 9, do so before adding this version. AGP 9 requires JDK 17 to build.

#### Custom `SignalCache` implementations must add `addAll()`

If you wrote a custom class implementing `SignalCache`, add:

```kotlin
override fun addAll(signals: List<Signal>) {
    signals.forEach { add(it) }
}
```

#### Custom `TelemetryDeckClient` implementations must add `flush()`

If you directly implement `TelemetryDeckClient`, add:

```kotlin
override suspend fun flush() { /* no-op or your own implementation */ }
```

### New Features

#### Force-flush the signal queue on demand

Send all queued signals immediately, without waiting for the next timer tick:

```kotlin
// From a coroutine or suspend function
TelemetryDeck.flush()
```

Useful before app exit, after a critical action, or during testing.

#### Configurable transmit interval and exponential backoff

Two new builder options tune how the SDK retries failed batches:

```kotlin
TelemetryDeck.start(
    context,
    TelemetryDeck.Builder()
        .appID("your-app-id")
        .transmitInterval(10_000L)      // first attempt: 10 s (default)
        .maxBackoffInterval(300_000L)   // maximum wait: 5 min (default)
)
```

When sending fails (e.g. no connection), the retry interval now doubles each time up to the cap, instead of retrying every 10 seconds. The counter resets as soon as a batch succeeds.

#### Signal cache now has a built-in cap

Both the in-memory and on-disk caches are capped at 10,000 signals by default. If signals are generated faster than they can be sent, the oldest unsent signals are dropped rather than growing unbounded. Customise via the cache constructor's `cacheLimit`.

#### Faster, non-blocking disk writes for the persistent cache

The persistent cache writes to disk asynchronously. Rapid `signal()` calls no longer block the caller while JSON is serialised.

### Behaviour Changes

#### Session creation timing

The session manager now registers after all other providers. Previously, when `start()` ran while the app was already in the foreground, the first `TelemetryDeck.Session.started` signal could be emitted before the other providers had registered — so it was missing enrichment such as SDK version and calendar parameters — and could be sent twice. It is now emitted once, fully enriched.

Note: `TelemetryDeck.sessionID` is populated when the process first reaches the foreground, so it is `null` if read synchronously inside `Application.onCreate()` immediately after `start()`. Session tracking is tied to the app lifecycle.

#### "Used last month" is now a fixed 30-day window

`TelemetryDeck.Retention.distinctDaysUsedLastMonth` now counts distinct usage days within the previous 30 days (was: current calendar month). This aligns with the KotlinSDK.

### Dependency Updates

| Library | 6.3.0 | 7.0.0 |
|---|---|---|
| Kotlin | 2.0.20 | 2.3.21 |
| AGP | 8.8.0 | 9.2.1 |
| Gradle | 8.10.2 | 9.5.0 |
| Ktor | 3.0.2 | 3.3.2 |
| kotlinx.serialization | 1.7.3 | 1.9.0 |
| kotlinx.coroutines | 1.10.1 | 1.10.2 |
| kotlinx.datetime | 0.6.2 | 0.7.1 |
| Google Play Billing | 7.1.1 | 8.3.0 |

### Migration Summary

| What | Action required | Who is affected |
|---|---|---|
| minSdk raised to 23 | Raise your app's `minSdk` or stay on 6.x | Apps supporting Android 5 |
| AGP 9 + Gradle 9 | Follow the AGP 9 migration guide | All |
| `SignalCache.addAll()` | Add the method to your implementation | Custom `SignalCache` implementors |
| `TelemetryDeckClient.flush()` | Add a `flush()` override | Custom `TelemetryDeckClient` implementors |
| `PersistentSignalCache` constructor | Use named params or the `(cacheDir, logger)` secondary constructor | Direct primary-constructor call sites |
