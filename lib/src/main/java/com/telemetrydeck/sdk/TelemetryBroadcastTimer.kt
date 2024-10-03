package com.telemetrydeck.sdk

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ticker
import java.lang.ref.WeakReference

internal class TelemetryBroadcastTimer(private val manager: WeakReference<TelemetryDeckSignalProcessor>, debugLogger: WeakReference<DebugLogger>) {

    // broadcast begins with a 10s delay after initialization and fires every 10s.
    private val timerChannel = ticker(delayMillis = 10_000, initialDelayMillis = 10_000)
    private var logger: DebugLogger? = null

    init {
        this.logger = debugLogger.get()
    }
    private var job: Job? = null

    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            job?.cancel()
            job?.join()
            job = launch {
                for (event in timerChannel) {
                    val managerInstance = manager.get()
                        ?: // can't broadcast without a manager to provide signals
                        continue

                    val cache = managerInstance.signalCache

                    if (cache == null) {
                        logger?.debug("Signal cache is not available")
                        continue
                    }

                    val signals =  cache.empty()
                    if (signals.isEmpty()) {
                        // no signals to broadcast
                        logger?.debug("Signal cache is empty, no signals to broadcast")
                        continue
                    }

                    logger?.debug("Broadcasting ${signals.count()} cached signals")
                    if (managerInstance.sendAll(signals).isFailure) {
                        logger?.debug("Failed to broadcast cached signals, re-enqueueing ${signals.count()} signals for later")
                        for (failedSignal in signals) {
                            cache.add(failedSignal)
                        }
                    }
                }
            }
        }
        logger?.debug("Started timer broadcast")
    }

    fun stop() {
        CoroutineScope(Dispatchers.IO).launch {
            job?.cancel()
        }
    }
}