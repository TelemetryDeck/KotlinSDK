package com.telemetrydeck.sdk

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.telemetrydeck.sdk.params.Calendar
import com.telemetrydeck.sdk.params.SDK
import com.telemetrydeck.sdk.signals.Session
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Regression test for the launch-enrichment bug where TelemetryDeck.Session.started was emitted
 * before enrichment providers (EnvironmentParameterProvider, CalendarParameterProvider, etc.) had
 * registered, resulting in a launch signal with missing device/SDK/calendar parameters and
 * sometimes a duplicate signal.
 *
 * The fix registers sessionManager LAST inside installProviders(), and removes the explicit
 * handleOnForeground() call from SessionTrackingSignalProvider.register() — the lifecycle
 * observer's onStart is the sole trigger.
 *
 * This test runs on a real device so ProcessLifecycleOwner transitions naturally when an Activity
 * is resumed, without any reflection hacks.
 */
@RunWith(AndroidJUnit4::class)
class SessionTrackingLaunchEnrichmentTest {

    private var scenario: ActivityScenario<ComponentActivity>? = null
    private var sdk: TelemetryDeck? = null

    @After
    fun cleanup() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            TelemetryDeck.stop()
            sdk?.let { instance ->
                instance.broadcastTimer?.stop()
                for (provider in instance.providers) {
                    provider.stop()
                }
                instance.identityProvider.stop()
                instance.sessionManager?.stop()
            }
            sdk = null
        }
        scenario?.close()
        scenario = null
        deleteTrackingFile()
    }

    @Test
    fun launch_session_signal_contains_enrichment_params_and_is_not_duplicated() {
        deleteTrackingFile()

        scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario!!.moveToState(Lifecycle.State.RESUMED)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertTrue(
                "ProcessLifecycleOwner must be at least STARTED before building SDK",
                ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            )
        }

        val cache = MemorySignalCache()

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
            sdk = TelemetryDeck.Builder()
                .appID("32CB6574-6732-4238-879F-582FEBEB6536")
                .sendNewSessionBeganSignal(true)
                .signalCache(cache)
                .build(appContext)
        }

        val allSignals = cache.empty()
        val sessionStartedSignals = allSignals.filter { it.type == Session.Started.signalName }

        assertEquals(
            "Expected exactly one ${Session.Started.signalName} signal, but got ${sessionStartedSignals.size}. All signals: ${allSignals.map { it.type }}",
            1,
            sessionStartedSignals.size
        )

        val launchSignal = sessionStartedSignals.first()

        val sdkVersionEntry = launchSignal.payload.find { it.startsWith("${SDK.Version.paramName}:") }
        assertNotNull(
            "Launch session signal must contain '${SDK.Version.paramName}:...' — enrichment providers were not registered before sessionManager",
            sdkVersionEntry
        )

        val calendarEntry = launchSignal.payload.find { it.startsWith("${Calendar.DayOfWeek.paramName}:") }
        assertNotNull(
            "Launch session signal must contain '${Calendar.DayOfWeek.paramName}:...' — CalendarParameterProvider was not registered before sessionManager",
            calendarEntry
        )
    }

    private fun deleteTrackingFile() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val file = File(appContext.filesDir, "telemetrydeckstracking")
        if (file.exists()) {
            file.delete()
        }
    }
}
