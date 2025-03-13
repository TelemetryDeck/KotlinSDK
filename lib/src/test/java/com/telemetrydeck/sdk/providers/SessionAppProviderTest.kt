package com.telemetrydeck.sdk.providers


import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LifecycleOwner
import com.telemetrydeck.sdk.TelemetryDeck
import com.telemetrydeck.sdk.signals.Session
import io.mockk.mockk
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class SessionAppProviderTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()


    private fun testDefaultTelemetryManager(): TelemetryDeck {
        val builder = TelemetryDeck.Builder()
        return builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .build(null)
    }

    private fun testTelemetryManager(sendNewSessionBeganSignal: Boolean): TelemetryDeck {
        val builder = TelemetryDeck.Builder()
        return builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .sendNewSessionBeganSignal(sendNewSessionBeganSignal)
            .build(null)
    }

    @Test
    fun sessionProvider_default_configuration_onStart_sends_newSessionBegan() {
        val lifecycleOwner: LifecycleOwner = mockk<LifecycleOwner>()
        val sut = SessionAppProvider()
        val manager = testDefaultTelemetryManager()
        sut.register(null, manager)

        sut.onStart(lifecycleOwner)

        Assert.assertEquals(1, manager.cache?.count())
        Assert.assertEquals(Session.Started.signalName, manager.cache?.empty()?.get(0)?.type)
    }

    @Test
    fun sessionProvider_sendNewSessionBeganSignal_onStart_sends_newSessionBegan() {
        val lifecycleOwner: LifecycleOwner = mockk<LifecycleOwner>()
        val sut = SessionAppProvider()
        val manager = testTelemetryManager(true)
        sut.register(null, manager)

        sut.onStart(lifecycleOwner)

        Assert.assertEquals(1, manager.cache?.count())
        Assert.assertEquals(Session.Started.signalName, manager.cache?.empty()?.get(0)?.type)
    }

    @Test
    fun sessionProvider_not_sendNewSessionBeganSignal_onStart_no_signals() {
        val lifecycleOwner: LifecycleOwner = mockk<LifecycleOwner>()
        val sut = SessionAppProvider()
        val manager = testTelemetryManager(false)
        sut.register(null, manager)

        sut.onStart(lifecycleOwner)

        Assert.assertEquals(0, manager.cache?.count())
    }
}