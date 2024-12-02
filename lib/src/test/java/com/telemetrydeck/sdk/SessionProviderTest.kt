package com.telemetrydeck.sdk

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LifecycleOwner
import io.mockk.mockk
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@Suppress("DEPRECATION")
class SessionProviderTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()


    private fun testDefaultTelemetryManager(): TelemetryManager {
        val builder = TelemetryManager.Builder()
        return builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .build(null)
    }

    private fun testTelemetryManager(sendNewSessionBeganSignal: Boolean): TelemetryManager {
        val builder = TelemetryManager.Builder()
        return builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .sendNewSessionBeganSignal(sendNewSessionBeganSignal)
            .build(null)
    }

    @Test
    fun sessionProvider_default_configuration_onStart_sends_newSessionBegan() {
        val lifecycleOwner: LifecycleOwner = mockk<LifecycleOwner>()
        val sut = SessionProvider()
        val manager = testDefaultTelemetryManager()
        sut.register(null, manager)

        sut.onStart(lifecycleOwner)

        Assert.assertEquals(1, manager.cache?.count())
        Assert.assertEquals(SignalType.NewSessionBegan.type, manager.cache?.empty()?.get(0)?.type)
    }

    @Test
    fun sessionProvider_sendNewSessionBeganSignal_onStart_sends_newSessionBegan() {
        val lifecycleOwner: LifecycleOwner = mockk<LifecycleOwner>()
        val sut = SessionProvider()
        val manager = testTelemetryManager(true)
        sut.register(null, manager)

        sut.onStart(lifecycleOwner)

        Assert.assertEquals(1, manager.cache?.count())
        Assert.assertEquals(SignalType.NewSessionBegan.type, manager.cache?.empty()?.get(0)?.type)
    }

    @Test
    fun sessionProvider_not_sendNewSessionBeganSignal_onStart_no_signals() {
        val lifecycleOwner: LifecycleOwner = mockk<LifecycleOwner>()
        val sut = SessionProvider()
        val manager = testTelemetryManager(false)
        sut.register(null, manager)

        sut.onStart(lifecycleOwner)

        Assert.assertEquals(0, manager.cache?.count())
    }


    @Test
    fun sessionProvider_default_configuration_onStop_resets_the_sessionID() {
        val lifecycleOwner: LifecycleOwner = mockk<LifecycleOwner>()
        val sut = SessionProvider()
        val manager = testDefaultTelemetryManager()
        sut.register(null, manager)

        val initialSessionID = manager.configuration.sessionID
        sut.onStop(lifecycleOwner)
        val nextSessionID = manager.configuration.sessionID

        Assert.assertNotEquals(initialSessionID, nextSessionID)
    }

    @Test
    fun sessionProvider_sendNewSessionBeganSignal_onStop_resets_the_sessionID() {
        val lifecycleOwner: LifecycleOwner = mockk<LifecycleOwner>()
        val sut = SessionProvider()
        val manager = testTelemetryManager(true)
        sut.register(null, manager)

        val initialSessionID = manager.configuration.sessionID
        sut.onStop(lifecycleOwner)
        val nextSessionID = manager.configuration.sessionID

        Assert.assertNotEquals(initialSessionID, nextSessionID)
    }

    @Test
    fun sessionProvider_not_sendNewSessionBeganSignal_onStop_keeps_the_sessionID() {
        val lifecycleOwner: LifecycleOwner = mockk<LifecycleOwner>()
        val sut = SessionProvider()
        val manager = testTelemetryManager(false)
        sut.register(null, manager)

        val initialSessionID = manager.configuration.sessionID
        sut.onStop(lifecycleOwner)
        val nextSessionID = manager.configuration.sessionID

        Assert.assertEquals(initialSessionID, nextSessionID)
    }
}