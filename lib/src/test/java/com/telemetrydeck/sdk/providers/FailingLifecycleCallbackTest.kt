package com.telemetrydeck.sdk.providers

import com.telemetrydeck.sdk.DebugLogger
import com.telemetrydeck.sdk.Signal
import com.telemetrydeck.sdk.SignalCache
import com.telemetrydeck.sdk.TelemetryDeck
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.TelemetryManagerConfiguration
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FailingLifecycleCallbackTest {

    private val failingCache = object : SignalCache {
        override fun add(signal: Signal) = throw IllegalStateException("cache add failed")
        override fun addAll(signals: List<Signal>) = throw IllegalStateException("cache addAll failed")
        override fun empty(): List<Signal> = emptyList()
        override fun count(): Int = 0
    }

    private val errors = mutableListOf<String>()

    private val recordingLogger = object : DebugLogger {
        override fun error(message: String) {
            errors.add(message)
        }
        override fun debug(message: String) {}
        override fun configure(enabled: Boolean) {}
    }

    private val throwingDebugLogger = object : DebugLogger {
        override fun error(message: String) {
            errors.add(message)
        }
        override fun debug(message: String) = throw IllegalStateException("provider body failed")
        override fun configure(enabled: Boolean) {}
    }

    private fun buildManager(): TelemetryDeck = TelemetryDeck.Builder()
        .appID("32CB6574-6732-4238-879F-582FEBEB6536")
        .sendNewSessionBeganSignal(true)
        .signalCache(failingCache)
        .logger(recordingLogger)
        .build(RuntimeEnvironment.getApplication())

    private fun assertLogged(provider: String, callback: String, cause: String) {
        Assert.assertTrue(errors.any { it.contains(provider) && it.contains(".$callback(") && it.contains(cause) })
    }

    @Test
    fun sessionApp_onStart_survives_failing_signal_cache() {
        val manager = buildManager()
        val sut = SessionAppProvider()
        sut.register(null, manager)
        errors.clear()

        sut.onStart(mockk(relaxed = true))

        assertLogged("SessionAppProvider", "onStart", "cache add failed")
    }

    @Test
    fun sessionApp_onStop_survives_failing_session_reset() {
        val processor = mockk<TelemetryDeckSignalProcessor>(relaxed = true)
        every { processor.configuration } returns TelemetryManagerConfiguration(UUID.randomUUID())
        every { processor.debugLogger } returns recordingLogger
        every { processor.resetSession(any()) } answers { throw IllegalStateException("reset failed") }
        val sut = SessionAppProvider()
        sut.register(null, processor)
        errors.clear()

        sut.onStop(mockk(relaxed = true))

        assertLogged("SessionAppProvider", "onStop", "reset failed")
    }

    @Test
    fun sessionTracking_onStart_survives_failing_signal_cache() {
        val manager = buildManager()
        val sut = SessionTrackingSignalProvider()
        sut.register(null, manager)
        errors.clear()

        sut.onStart(mockk(relaxed = true))

        assertLogged("SessionTrackingSignalProvider", "onStart", "cache add failed")
    }

    @Test
    fun sessionTracking_lifecycle_callbacks_survive_failure_in_provider_body() {
        val manager = buildManager()
        val sut = SessionTrackingSignalProvider()
        sut.register(null, manager)
        manager.logger = throwingDebugLogger
        errors.clear()

        sut.onStart(mockk(relaxed = true))
        sut.onStop(mockk(relaxed = true))

        assertLogged("SessionTrackingSignalProvider", "onStart", "provider body failed")
        assertLogged("SessionTrackingSignalProvider", "onStop", "provider body failed")
    }

    @Test
    fun durationTracking_lifecycle_callbacks_survive_failure_in_provider_body() {
        val manager = buildManager()
        val sut = DurationSignalTrackerProvider()
        sut.register(null, manager)
        manager.logger = throwingDebugLogger
        errors.clear()

        sut.onStart(mockk(relaxed = true))
        sut.onStop(mockk(relaxed = true))

        assertLogged("DurationSignalTrackerProvider", "onStart", "provider body failed")
        assertLogged("DurationSignalTrackerProvider", "onStop", "provider body failed")
    }

    @Test
    fun memorySession_onStart_survives_failing_signal_cache() {
        val manager = buildManager()
        val sut = MemorySessionProvider()
        sut.register(null, manager)
        errors.clear()

        sut.onStart(mockk(relaxed = true))

        assertLogged("MemorySessionProvider", "onStart", "cache add failed")
    }
}
