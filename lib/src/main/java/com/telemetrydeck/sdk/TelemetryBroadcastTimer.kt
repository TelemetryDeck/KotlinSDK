package com.telemetrydeck.sdk

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ticker
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.abs

internal class TelemetryBroadcastTimer(private val manager: WeakReference<TelemetryManager>, debugLogger: WeakReference<DebugLogger>) {

    // broadcast begins with a 10s delay after initialization and fires every 10s.
    private val timerChannel = ticker(delayMillis = 10_000, initialDelayMillis = 10_000)
    private var logger: DebugLogger? = null

    init {
        this.logger = debugLogger.get()
    }
    private var job: Job? = null

    companion object {
        fun filterOldSignals(signals: List<Signal>): List<Signal> {
            val now = Date().time
            return signals.filter {
                // ignore signals older than 24h
                (abs(now - it.receivedAt.time) / 1000) <= 24 * 60 * 60
            }
        }
    }

    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            job?.cancel()
            job?.join()
            job = launch {
                for (event in timerChannel) {
                    val managerInstance = manager.get()
                        ?: // can't broadcast without a manager to provide signals
                        continue

                    val signals =  filterOldSignals(managerInstance.cache?.empty() ?: emptyList())
                    if (signals.isEmpty()) {
                        // no signals to broadcast
                        logger?.debug("No signals to broadcast")
                        continue
                    }

                    logger?.debug("Broadcasting ${signals.count()} queued signals")
                    if (managerInstance.send(signals).isFailure) {
                        logger?.debug("Re-enqueueing ${signals.count()} signals")
                        for (failedSignal in signals) {
                            managerInstance.cache?.add(failedSignal)
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