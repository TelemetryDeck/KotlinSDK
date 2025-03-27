package com.telemetrydeck.sdk.providers

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.telemetrydeck.sdk.DateSerializer
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.providers.helpers.restoreStateFromDisk
import com.telemetrydeck.sdk.providers.helpers.writeStateToDisk
import com.telemetrydeck.sdk.signals.Signal
import kotlinx.serialization.Serializable
import java.lang.ref.WeakReference
import java.util.Date

class DurationSignalTrackerProvider : TelemetryDeckProvider, DefaultLifecycleObserver {
    private var appContext: WeakReference<Context?>? = null
    private var manager: WeakReference<TelemetryDeckSignalProcessor>? = null
    private var state: TrackerState? = null
    private val fileName = "telemetrydeckduration"
    private val fileEncoding = Charsets.UTF_8

    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        this.manager = WeakReference(client)
        this.appContext = WeakReference(ctx)
        this.state = restoreStateFromDisk<TrackerState?>(
            this.appContext?.get(),
            this.fileName,
            this.fileEncoding,
            this.manager?.get()?.debugLogger
        ) ?: TrackerState(emptyMap())
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

    fun handleOnForeground(now: Date = Date()) {
        this.manager?.get()?.debugLogger?.debug("Commencing signal duration tracking")

        val preRestoreState = this.state
        if (preRestoreState == null) {
            // no state present in memory, restore it
            val restoredState = restoreStateFromDisk<TrackerState?>(
                this.appContext?.get(),
                this.fileName,
                this.fileEncoding,
                this.manager?.get()?.debugLogger
            )
            this.state = restoredState
        }

        var currentState = this.state ?: TrackerState(emptyMap())

        val lastEnteredBackground = currentState.lastEnteredBackground
        if (lastEnteredBackground != null) {
            // subtracts background time from all signals by moving their start time forward
            val backgroundDuration = now.time - lastEnteredBackground.time
            this.manager?.get()?.debugLogger?.debug("Adapting signal duration with -$backgroundDuration ms")
            currentState = currentState.copy(
                signals = currentState.signals.mapValues {
                    if (it.value.includeBackgroundTime == true) {
                        it.value.copy()
                    } else {
                        it.value.copy(
                            startTime = Date(it.value.startTime.time + backgroundDuration)
                        )
                    }
                }
            )
        }

        writeStateToDisk(currentState, this.appContext?.get(), this.fileName, this.fileEncoding, this.manager?.get()?.debugLogger)
        this.state = currentState
    }

    fun handleOnBackground(now: Date = Date()) {
        this.manager?.get()?.debugLogger?.debug("Signal duration tracking is shutting down")
        val currentState = this.state?.copy(
            lastEnteredBackground = now
        )
        if (currentState != null) {
            this.state = currentState
            writeStateToDisk(currentState, this.appContext?.get(), this.fileName, this.fileEncoding, this.manager?.get()?.debugLogger)
        }
    }

    @Synchronized
    fun startTracking(signalName: String, parameters: Map<String, String>, includeBackgroundTime: Boolean, now: Date = Date()) {
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
    fun stopTracking(signalName: String, parameters: Map<String, String>, now: Date = Date()): Map<String, String>? {
        val currentState = state ?: throw Exception("stopTracking called before register")
        val signalTracking = currentState.signals[signalName]
        if (signalTracking == null) {
            this.manager?.get()?.debugLogger?.error("stopTracking called for untracked signal $signalName. Did you forget to call startTracking?")
            return null
        }
        // stop tracking the signal name
        this.state = currentState.copy(signals = currentState.signals - signalName)

        // queue a duration signal for sending
        val trackingDurationMs = now.time - signalTracking.startTime.time
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
        val lastEnteredBackground: Date? = null
    )

    @Serializable
    data class CachedData(
        @Serializable(with = DateSerializer::class)
        val startTime: Date,
        val parameters: Map<String, String>,
        val includeBackgroundTime: Boolean?
    )
}
