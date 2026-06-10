package com.telemetrydeck.sdk.providers

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.telemetrydeck.sdk.TelemetryDeck
import com.telemetrydeck.sdk.signals.Session
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Regression guard for the launch-session ordering bug:
 *
 * When the process lifecycle is already at STARTED when TelemetryDeck.build() runs,
 * SessionTrackingSignalProvider.register() -> ProcessLifecycleOwner.addObserver() dispatches
 * a synchronous catch-up onStart() -> Session.started is emitted at that point. The fix ensures
 * all enrichment providers (EnvironmentParameterProvider, CalendarParameterProvider, etc.) are
 * registered before sessionManager.register() runs, so the signal is fully enriched and emitted
 * exactly once.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SessionTrackingRegressionTest {

    @Before
    fun setUp() {
        TelemetryDeck.instance = null
    }

    @After
    fun tearDown() {
        TelemetryDeck.stop()
        TelemetryDeck.instance = null
        // Reset ProcessLifecycleOwner to below STARTED so tests sharing this sandbox are not
        // affected. activityStopped$lifecycle_process() mirrors what happens when the last
        // activity stops, transitioning the registry back toward CREATED.
        val processOwner = ProcessLifecycleOwner.get()
        processOwner.javaClass
            .getMethod("activityStopped\$lifecycle_process")
            .invoke(processOwner)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    @Test
    fun launch_sessionStarted_is_enriched_and_single_when_process_already_started() {
        // Advance ProcessLifecycleOwner to STARTED before build() is called, reproducing the
        // scenario where an app's process is already foregrounded when TelemetryDeck initializes.
        // activityStarted$lifecycle_process() is the exact method that ReportFragment.onStart()
        // and the ActivityLifecycleCallbacks in ProcessLifecycleOwner.attach() invoke in
        // production. It is public at the JVM level (Kotlin-module visibility only prevents
        // cross-module calls at the Kotlin compiler level).
        val processOwner = ProcessLifecycleOwner.get()
        processOwner.javaClass
            .getMethod("activityStarted\$lifecycle_process")
            .invoke(processOwner)
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        Assert.assertTrue(
            "ProcessLifecycleOwner must reach STARTED before build() for this test to be meaningful",
            processOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        )

        val manager = TelemetryDeck.Builder()
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .sendNewSessionBeganSignal(true)
            .build(RuntimeEnvironment.getApplication())

        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        val signals = manager.cache?.empty() ?: emptyList()
        val sessionStartedSignals = signals.filter { it.type == Session.Started.signalName }

        Assert.assertEquals(
            "Expected exactly one Session.started signal, got ${sessionStartedSignals.size}",
            1,
            sessionStartedSignals.size
        )

        val signal = sessionStartedSignals.first()
        Assert.assertTrue(
            "Session.started must carry TelemetryDeck.SDK.version from EnvironmentParameterProvider",
            signal.payload.any { it.startsWith("TelemetryDeck.SDK.version:") }
        )
        Assert.assertTrue(
            "Session.started must carry TelemetryDeck.Calendar.dayOfWeek from CalendarParameterProvider",
            signal.payload.any { it.startsWith("TelemetryDeck.Calendar.dayOfWeek:") }
        )
    }
}
