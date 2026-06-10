package com.telemetrydeck.sdk

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.lang.ref.WeakReference
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryBroadcastTimerTest {

    private fun buildTimer(
        transmitInterval: Long = 10_000L,
        maxBackoffInterval: Long = 300_000L,
    ): TelemetryBroadcastTimer {
        val processor = mockk<TelemetryDeckSignalProcessor>(relaxed = true)
        val logger = mockk<DebugLogger>(relaxed = true)
        return TelemetryBroadcastTimer(
            manager = WeakReference(processor),
            debugLogger = WeakReference(logger),
            transmitInterval = transmitInterval,
            maxBackoffInterval = maxBackoffInterval,
        )
    }

    @Test
    fun backoffGrowsExponentially() {
        val sut = buildTimer(transmitInterval = 10_000L)

        Assert.assertEquals(10_000L, sut.nextInterval(0))
        Assert.assertEquals(20_000L, sut.nextInterval(1))
        Assert.assertEquals(40_000L, sut.nextInterval(2))
        Assert.assertEquals(80_000L, sut.nextInterval(3))
    }

    @Test
    fun backoffCapsAtMax() {
        val sut = buildTimer(transmitInterval = 10_000L, maxBackoffInterval = 300_000L)

        Assert.assertEquals(300_000L, sut.nextInterval(10))
    }

    @Test
    fun successResetsBackoff() {
        val sut = buildTimer(transmitInterval = 10_000L)

        sut.resetBackoff()
        Assert.assertEquals(10_000L, sut.nextInterval())
    }

    @Test
    fun resetBackoffClearsCounter() {
        val sut = buildTimer(transmitInterval = 10_000L)

        sut.resetBackoff()
        Assert.assertEquals(0, sut.currentBackoffFailures())
        Assert.assertEquals(10_000L, sut.nextInterval())
    }

    @Test
    fun clampOnFailureCountAvoidsLongOverflow() {
        val sut = buildTimer(transmitInterval = 10_000L, maxBackoffInterval = 300_000L)

        Assert.assertEquals(300_000L, sut.nextInterval(Int.MAX_VALUE))
    }

    @Test
    fun failedBatchReenqueuesViaAddAll() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val cache = mockk<SignalCache>(relaxed = true)
        val failedSignals = listOf(
            Signal(UUID.randomUUID(), "type1", "user", SignalPayload()),
            Signal(UUID.randomUUID(), "type2", "user", SignalPayload()),
        )
        every { cache.empty() } returns failedSignals

        val processor = mockk<TelemetryDeckSignalProcessor>(relaxed = true)
        every { processor.signalCache } returns cache
        coEvery { processor.sendAll(any()) } returns Result.failure(Exception("network error"))

        val logger = mockk<DebugLogger>(relaxed = true)
        val timer = TelemetryBroadcastTimer(
            manager = WeakReference(processor),
            debugLogger = WeakReference(logger),
            transmitInterval = 50L,
            coroutineContext = testDispatcher,
        )

        timer.start()
        advanceTimeBy(200L)
        timer.stop()
        advanceUntilIdle()

        verify(atLeast = 1) { cache.addAll(failedSignals) }
        verify(exactly = 0) { cache.add(any()) }
    }
}
