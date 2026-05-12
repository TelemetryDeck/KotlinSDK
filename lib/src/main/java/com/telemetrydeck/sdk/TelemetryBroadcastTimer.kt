package com.telemetrydeck.sdk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

internal class TelemetryBroadcastTimer(
    private val manager: WeakReference<TelemetryDeckSignalProcessor>,
    debugLogger: WeakReference<DebugLogger>,
    private val transmitInterval: Long = 10_000L,
    private val maxBackoffInterval: Long = 300_000L,
    coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.IO,
) {
    private var logger: DebugLogger? = null
    private val consecutiveFailures = AtomicInteger(0)
    private val scope = CoroutineScope(coroutineContext)
    private var job: Job? = null

    init {
        this.logger = debugLogger.get()
    }

    internal fun nextInterval(): Long = nextInterval(consecutiveFailures.get())

    internal fun nextInterval(failures: Int): Long {
        if (failures == 0) return transmitInterval
        // Clamp the exponent to 16 to guard against Long overflow; maxBackoffInterval caps the result in practice.
        val factor = 1L shl minOf(failures, 16)
        return minOf(transmitInterval * factor, maxBackoffInterval)
    }

    internal fun resetBackoff() {
        consecutiveFailures.set(0)
    }

    internal fun currentBackoffFailures(): Int = consecutiveFailures.get()

    fun start() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                delay(nextInterval())
                transmitBatch()
            }
        }
        logger?.debug("Started timer broadcast")
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    /**
     * Immediately transmits all cached events without waiting for the next timer tick, resets the backoff counter to 0 before transmitting.
     */
    internal suspend fun flush() {
        consecutiveFailures.set(0)
        transmitBatch()
    }

    private suspend fun transmitBatch() {
        val managerInstance = manager.get() ?: return
        val cache = managerInstance.signalCache
        if (cache == null) {
            logger?.debug("Signal cache is not available")
            return
        }
        val signals = cache.empty()
        if (signals.isEmpty()) {
            logger?.debug("Signal cache is empty, no signals to broadcast")
            return
        }
        logger?.debug("Broadcasting ${signals.count()} cached signals")
        if (managerInstance.sendAll(signals).isFailure) {
            logger?.debug("Failed to broadcast cached signals, re-enqueueing ${signals.count()} signals for later")
            consecutiveFailures.incrementAndGet()
            cache.addAll(signals)
        } else {
            consecutiveFailures.set(0)
        }
    }
}
