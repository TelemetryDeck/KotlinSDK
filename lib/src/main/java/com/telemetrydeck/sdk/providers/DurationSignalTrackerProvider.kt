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
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun stop() {

    }

    override fun onStart(owner: LifecycleOwner) {
        handleOnStart()
    }

    override fun onStop(owner: LifecycleOwner) {
        handleOnStop()
    }

    fun handleOnStart() {
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
            val now = Date()
            val backgroundDuration = now.time - lastEnteredBackground.time
            this.manager?.get()?.debugLogger?.debug("Adapting signal duration with -$backgroundDuration ms")
            currentState = currentState.copy(
                signals = currentState.signals.mapValues {
                    it.value.copy(
                        startTime = Date(it.value.startTime.time + backgroundDuration)
                    )
                }
            )
        }

        writeStateToDisk(currentState, this.appContext?.get(), this.fileName, this.fileEncoding, this.manager?.get()?.debugLogger)
        this.state = currentState
    }

    fun handleOnStop() {
        this.manager?.get()?.debugLogger?.debug("Signal duration tracking is shutting down")
        val currentState = this.state
        if (currentState != null) {
            val updatedState = currentState.copy(
                lastEnteredBackground = Date()
            )
            writeStateToDisk(updatedState, this.appContext?.get(), this.fileName, this.fileEncoding, this.manager?.get()?.debugLogger)
        }
    }

    @Synchronized
    fun startTracking(signalName: String, parameters: Map<String, String>) {
        val currentState = state ?: throw Exception("startTracking called before register")
        val now = Date()
        this.state = currentState.copy(
            signals = currentState.signals + (signalName to CachedData(
                now,
                parameters
            ))
        )
    }

    @Synchronized
    fun stopTracking(signalName: String, parameters: Map<String, String>): Map<String, String>? {
        val currentState = state ?: throw Exception("stopTracking called before register")
        val signalTracking = currentState.signals[signalName]
        if (signalTracking == null) {
            this.manager?.get()?.debugLogger?.error("stopTracking called for untracked signal $signalName. Did you forget to call startTracking?")
            return null
        }
        // stop tracking the signal name
        this.state = currentState.copy(signals = currentState.signals - signalName)

        // queue a duration signal for sending
        val trackingDurationMs = Date().time - signalTracking.startTime.time
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
        val parameters: Map<String, String>
    )
}
