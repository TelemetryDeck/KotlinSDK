package com.telemetrydeck.sdk.providers

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.telemetrydeck.sdk.DateSerializer
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.signals.Signal
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
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
        this.state = restoreStateFromDisk() ?: TrackerState(emptyMap())
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
        val preRestoreState = this.state
        if (preRestoreState == null) {
            // no state present in memory, restore it
            val restoredState = restoreStateFromDisk()
            this.state = restoredState
        }

        var currentState = this.state
            ?: // nothing to do, probably not started yet
            return

        val lastEnteredBackground = currentState.lastEnteredBackground
        if (lastEnteredBackground != null) {
            // subtracts background time from all signals by moving their start time forward
            val now = Date()
            val backgroundDuration = now.time - lastEnteredBackground.time
            currentState = currentState.copy(
                signals = currentState.signals.mapValues {
                    it.value.copy(
                        startTime = Date(it.value.startTime.time + backgroundDuration)
                    )
                }
            )
        }

        writeStateToDisk(currentState)
        this.state = currentState
    }

    fun handleOnStop() {
        val currentState = this.state
        if (currentState != null) {
            writeStateToDisk(currentState)
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

    private fun restoreStateFromDisk(): TrackerState? {
        val context = this.appContext?.get() ?: return null
        try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                val json = file.readText(fileEncoding)
                val state = Json.decodeFromString<TrackerState>(json)
                return state
            }
        } catch (e: Exception) {
            this.manager?.get()?.debugLogger?.error("failed to restore duration tracking state ${e.stackTraceToString()}")
        }
        return null
    }

    private fun writeStateToDisk(state: TrackerState) {
        val context = this.appContext?.get() ?: return
        try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                file.delete()
            }
            val json = Json.encodeToString(state)
            file.writeText(json, fileEncoding)
        } catch (e: Exception) {
            this.manager?.get()?.debugLogger?.error("failed to write duration tracking state ${e.stackTraceToString()}")
        }
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
