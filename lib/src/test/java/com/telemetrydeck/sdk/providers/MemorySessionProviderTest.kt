package com.telemetrydeck.sdk.providers

import com.telemetrydeck.sdk.TelemetryDeck
import com.telemetrydeck.sdk.signals.Acquisition
import com.telemetrydeck.sdk.signals.Session
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MemorySessionProviderTest {

    @Test
    fun sessionStarted_emitted_when_configured() {
        val context = RuntimeEnvironment.getApplication()
        val manager = TelemetryDeck.Builder()
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .inMemoryOnly(true)
            .sendNewSessionBeganSignal(true)
            .build(context)

        val sessionProvider = manager.sessionManager as? MemorySessionProvider
        Assert.assertNotNull(sessionProvider)
        sessionProvider!!.handleOnForeground()

        val signals = manager.cache?.empty() ?: emptyList()
        val sessionStartedSignals = signals.filter { it.type == Session.Started.signalName }

        Assert.assertEquals(1, sessionStartedSignals.size)
    }

    @Test
    fun sessionStarted_not_emitted_when_disabled() {
        val context = RuntimeEnvironment.getApplication()
        val manager = TelemetryDeck.Builder()
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .inMemoryOnly(true)
            .sendNewSessionBeganSignal(false)
            .build(context)

        val sessionProvider = manager.sessionManager as? MemorySessionProvider
        Assert.assertNotNull(sessionProvider)
        sessionProvider!!.handleOnForeground()

        val signals = manager.cache?.empty() ?: emptyList()
        val sessionStartedSignals = signals.filter { it.type == Session.Started.signalName }

        Assert.assertEquals(0, sessionStartedSignals.size)
    }

    @Test
    fun newInstallDetected_never_emitted() {
        val context = RuntimeEnvironment.getApplication()
        val manager = TelemetryDeck.Builder()
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .inMemoryOnly(true)
            .sendNewSessionBeganSignal(true)
            .build(context)

        val sessionProvider = manager.sessionManager as? MemorySessionProvider
        Assert.assertNotNull(sessionProvider)
        sessionProvider!!.handleOnForeground()

        val signals = manager.cache?.empty() ?: emptyList()
        val newInstallSignals = signals.filter { it.type == Acquisition.NewInstallDetected.signalName }

        Assert.assertEquals(0, newInstallSignals.size)
    }

    @Test
    fun sessionID_is_stable_after_repeated_foreground() {
        val context = RuntimeEnvironment.getApplication()
        val manager = TelemetryDeck.Builder()
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .inMemoryOnly(true)
            .build(context)

        val sessionProvider = manager.sessionManager as? MemorySessionProvider
        Assert.assertNotNull(sessionProvider)
        sessionProvider!!.handleOnForeground()
        val firstID = sessionProvider.getCurrentSessionID()

        sessionProvider.handleOnForeground()
        val secondID = sessionProvider.getCurrentSessionID()

        Assert.assertNotNull(firstID)
        Assert.assertEquals(firstID, secondID)
    }
}
