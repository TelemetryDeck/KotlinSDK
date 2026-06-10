package com.telemetrydeck.sdk.providers

import com.telemetrydeck.sdk.TelemetryDeck
import com.telemetrydeck.sdk.signals.Session
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SessionTrackingSignalProviderTest {

    @Test
    fun sessionStarted_emits_exactly_one_signal_when_foreground_triggered_after_registration() {
        val context = RuntimeEnvironment.getApplication()
        val manager = TelemetryDeck.Builder()
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .sendNewSessionBeganSignal(true)
            .build(context)

        val sessionProvider = manager.sessionManager as? SessionTrackingSignalProvider
        Assert.assertNotNull(sessionProvider)
        sessionProvider!!.handleOnForeground()

        val signals = manager.cache?.empty() ?: emptyList()
        val sessionStartedSignals = signals.filter { it.type == Session.Started.signalName }

        Assert.assertEquals(1, sessionStartedSignals.size)
    }

    @Test
    fun sessionStarted_payload_contains_sdk_version_and_calendar_enrichment() {
        val context = RuntimeEnvironment.getApplication()
        val manager = TelemetryDeck.Builder()
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .sendNewSessionBeganSignal(true)
            .build(context)

        val sessionProvider = manager.sessionManager as? SessionTrackingSignalProvider
        Assert.assertNotNull(sessionProvider)
        sessionProvider!!.handleOnForeground()

        val signals = manager.cache?.empty() ?: emptyList()
        val sessionStartedSignal = signals.firstOrNull { it.type == Session.Started.signalName }

        Assert.assertNotNull(sessionStartedSignal)
        Assert.assertTrue(
            sessionStartedSignal?.payload?.any { it.startsWith("TelemetryDeck.SDK.version:") } == true
        )
        Assert.assertTrue(
            sessionStartedSignal?.payload?.any { it.startsWith("TelemetryDeck.Calendar.dayOfWeek:") } == true
        )
    }

    @Test
    fun sessionStarted_not_emitted_when_disabled() {
        val context = RuntimeEnvironment.getApplication()
        val manager = TelemetryDeck.Builder()
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .sendNewSessionBeganSignal(false)
            .build(context)

        val sessionProvider = manager.sessionManager as? SessionTrackingSignalProvider
        Assert.assertNotNull(sessionProvider)
        sessionProvider!!.handleOnForeground()

        val signals = manager.cache?.empty() ?: emptyList()
        val sessionStartedSignals = signals.filter { it.type == Session.Started.signalName }

        Assert.assertEquals(0, sessionStartedSignals.size)
    }
}
