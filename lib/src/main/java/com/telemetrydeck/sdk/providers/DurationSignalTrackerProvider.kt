package com.telemetrydeck.sdk.providers

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.telemetrydeck.sdk.DateSerializer
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.TelemetryStorage
import com.telemetrydeck.sdk.readState
import com.telemetrydeck.sdk.signals.Signal
import com.telemetrydeck.sdk.writeState
import kotlinx.serialization.Serializable
import java.lang.ref.WeakReference
import kotlin.time.Clock
import kotlin.time.Instant

class DurationSignalTrackerProvider : TelemetryDeckProvider, DefaultLifecycleObserver {
    private var storage: TelemetryStorage? = null
    private var manager: WeakReference<TelemetryDeckSignalProcessor>? = null
    private var state: TrackerState? = null
    private val fileName = "telemetrydeckduration"

    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        this.manager = WeakReference(client)
        this.storage = client.storage
        this.state = storage?.readState<TrackerState>(fileName, manager?.get()?.debugLogger) ?: TrackerState(emptyMap())
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        handleOnForeground()
    }

    override fun onStop(owner: LifecycleOwner) {
        handleOnBackground()
    }

    fun handleOnForeground(now: Instant = Clock.System.now()) {
        this.manager?.get()?.debugLogger?.debug("Commencing signal duration tracking")

        val preRestoreState = this.state
        if (preRestoreState == null) {
            // no state present in memory, restore it
            this.state = storage?.readState<TrackerState>(fileName, manager?.get()?.debugLogger)
        }

        var currentState = this.state ?: TrackerState(emptyMap())

        val lastEnteredBackground = currentState.lastEnteredBackground
        if (lastEnteredBackground != null) {
            // subtracts background time from all signals by moving their start time forward
            val backgroundDuration = now - lastEnteredBackground
            this.manager?.get()?.debugLogger?.debug("Adapting signal duration with -${backgroundDuration.inWholeMilliseconds} ms")
            currentState = currentState.copy(
                signals = currentState.signals.mapValues {
                    if (it.value.includeBackgroundTime == true) {
                        it.value.copy()
                    } else {
                        it.value.copy(
                            startTime = it.value.startTime + backgroundDuration
                        )
                    }
                }
            )
        }

        storage?.writeState(fileName, currentState, manager?.get()?.debugLogger)
        this.state = currentState
    }

    fun handleOnBackground(now: Instant = Clock.System.now()) {
        this.manager?.get()?.debugLogger?.debug("Signal duration tracking is shutting down")
        val currentState = this.state?.copy(
            lastEnteredBackground = now
        )
        if (currentState != null) {
            this.state = currentState
            storage?.writeState(fileName, currentState, manager?.get()?.debugLogger)
        }
    }

    @Synchronized
    fun startTracking(signalName: String, parameters: Map<String, String>, includeBackgroundTime: Boolean, now: Instant = Clock.System.now()) {
        val currentState = state ?: throw Exception("startTracking called before register")
        this.state = currentState.copy(
            signals = currentState.signals + (signalName to CachedData(
                now,
                parameters,
                includeBackgroundTime
            ))
        )
    }

    @Synchronized
    fun stopTracking(signalName: String, parameters: Map<String, String>, now: Instant = Clock.System.now()): Map<String, String>? {
        val currentState = state ?: throw Exception("stopTracking called before register")
        val signalTracking = currentState.signals[signalName]
        if (signalTracking == null) {
            this.manager?.get()?.debugLogger?.error("stopTracking called for untracked signal $signalName. Did you forget to call startTracking?")
            return null
        }
        // stop tracking the signal name
        this.state = currentState.copy(signals = currentState.signals - signalName)

        // queue a duration signal for sending
        val trackingDurationMs = (now - signalTracking.startTime).inWholeMilliseconds
        val trackingDurationSec = trackingDurationMs / 1000.0

        val mergedParameters = mutableMapOf<String, String>()
        for (param in signalTracking.parameters) {
            mergedParameters[param.key] = param.value
        }
        for (param in parameters) {
            mergedParameters[param.key] = param.value
        }
        mergedParameters[Signal.DurationInSeconds.signalName] = "%.3f".format(trackingDurationSec)

       return mergedParameters
    }




    @Serializable
    data class TrackerState(
        val signals: Map<String, CachedData>,
        @Serializable(with = DateSerializer::class)
        val lastEnteredBackground: Instant? = null
    )

    @Serializable
    data class CachedData(
        @Serializable(with = DateSerializer::class)
        val startTime: Instant,
        val parameters: Map<String, String>,
        val includeBackgroundTime: Boolean?
    )
}
