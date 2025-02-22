package com.telemetrydeck.sdk.providers

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.telemetrydeck.sdk.DateSerializer
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.params.Retention
import com.telemetrydeck.sdk.providers.helpers.restoreStateFromDisk
import com.telemetrydeck.sdk.providers.helpers.writeStateToDisk
import com.telemetrydeck.sdk.signals.Acquisition
import kotlinx.serialization.Serializable
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
*
* Monitors the app's lifecycle in order to provide Retention and Acquisition parameters.
*
* This provider broadcasts the following additional signals:
* - `TelemetryDeck.Acquisition.newInstallDetected`
* - `TelemetryDeck.Session.started` (when `sendNewSessionBeganSignal` is set to `true`)
*
*/
class SessionTrackingSignalProvider: TelemetryDeckProvider, DefaultLifecycleObserver {
    private var appContext: WeakReference<Context?>? = null
    private var manager: WeakReference<TelemetryDeckSignalProcessor>? = null
    private var providerState: TrackingState? = null
    private val fileName = "telemetrydeckstracking"
    private val fileEncoding = Charsets.UTF_8
    private val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        this.manager = WeakReference(client)
        this.appContext = WeakReference(ctx)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    override fun enrich(
        signalType: String,
        clientUser: String?,
        additionalPayload: Map<String, String>
    ): Map<String, String> {
        val signalPayload = additionalPayload.toMutableMap()
        for (item in createMetadata()) {
            if (!signalPayload.containsKey(item.key)) {
                signalPayload[item.key] = item.value
            }
        }
        return signalPayload
    }

    private fun createMetadata(): Map<String, String> {
        val currentState = this.providerState?.copy() ?: return emptyMap()

        val attributes = mutableMapOf(
            com.telemetrydeck.sdk.params.Acquisition.FirstSessionDate.paramName to (currentState.distinctDays.firstOrNull() ?: ""),
            Retention.DistinctDaysUsed.paramName to "${currentState.distinctDays.size}",
            Retention.TotalSessionsCount.paramName to "${currentState.lifetimeSessionsCount ?: 0}",
        )

        if (currentState.sessions.isNotEmpty()) {
            val completedSessions = currentState.sessions.filter { it.ended != null }
            attributes[Retention.AverageSessionSeconds.paramName] =
                "${completedSessions.map { it.durationMillis / 1000 }.average().toInt()}"

            val lastCompletedSession = completedSessions.lastOrNull {it.ended != null }
            if (lastCompletedSession != null) {
                attributes[Retention.PreviousSessionSeconds.paramName] = "%.3f".format(lastCompletedSession.durationMillis / 1000.0)
            }
        }

        return attributes
    }

    override fun onStart(owner: LifecycleOwner) {
        handleOnForeground()
    }

    fun handleOnForeground(now: Date = Date()) {
        this.manager?.get()?.debugLogger?.debug("Commencing session tracking")
        val preRestoreState = this.providerState
        if (preRestoreState == null) {
            // no state present in memory, restore it
            val restoredState = restoreStateFromDisk<TrackingState?>(
                this.appContext?.get(),
                this.fileName,
                this.fileEncoding,
                this.manager?.get()?.debugLogger
            )
            this.providerState = restoredState
        }

        var currentState = this.providerState ?: TrackingState(emptyList(), emptyList())

        this.manager?.get()?.debugLogger?.debug("Session tracking restoring sessions")
        currentState = updateDistinctDays(currentState, now)

        if (currentState.sessions.isEmpty()) {
            // we're running for the first time
            this.manager?.get()?.debugLogger?.debug("Session tracking is running for the first time")
            val today = dateFormat.format(now)
            manager?.get()?.processSignal(
                Acquisition.NewInstallDetected.signalName,
                mapOf(
                    com.telemetrydeck.sdk.params.Acquisition.FirstSessionDate.paramName to today
                )
            )
        }

        if (shouldStartNewSession(currentState, now)) {
            currentState = endLastSessionIfPresent(currentState, now)
            currentState = startNewSessionAndTruncateStorage(currentState, now)
        }

        this.manager?.get()?.debugLogger?.debug("Session tracking ${currentState.sessions.size} sessions.")
        writeStateToDisk(currentState, this.appContext?.get(), this.fileName, this.fileEncoding, this.manager?.get()?.debugLogger)
        this.providerState = currentState
    }

    /**
     * Returns `true` if more than 5 minutes have expired since the app was sent in the background
     * */
    private fun shouldStartNewSession(state: TrackingState, now: Date): Boolean {
        val lastEnteredBackground = state.lastEnteredBackground
        if (lastEnteredBackground != null) {
            val backgroundDuration = now.time - lastEnteredBackground.time
            return backgroundDuration > 5 * 60 * 1000
        } else {
            return true
        }
    }

    private fun startNewSessionAndTruncateStorage(state: TrackingState, now: Date): TrackingState {
        val newSession = StoredSession(now, null, 0)
        var result = state.copy()
        val lifetimeSessionsCount = (result.lifetimeSessionsCount ?: 0) + 1
        result = result.copy(
            sessions = result.sessions + newSession,
            lifetimeSessionsCount = lifetimeSessionsCount,
            distinctDays = (result.distinctDays + dateFormat.format(now)).distinctBy { it.lowercase() }
        )

        // truncate started more than 90 days ago
        val cutOff = Date(Date().time - (90L * 24L * 60L * 60L * 1000L))
        val survivingSessions = result.sessions.filter { it.firstStart.after(cutOff) }

        result = result.copy(
            sessions = survivingSessions
        )

        return result
    }

    private fun endLastSessionIfPresent(state: TrackingState, now: Date): TrackingState {
        val lastSession = state.sessions.lastOrNull()
            ?: // nothing to do
            return state

        var result = state.copy()
        // mark the session as ended and calculate the duration
        this.manager?.get()?.debugLogger?.debug("Session tracking finalizing current session")

        val durationMillis = now.time - lastSession.firstStart.time
        result = result.copy(
            sessions = result.sessions.dropLast(1) + lastSession.copy(ended = now, durationMillis = durationMillis),
        )
        return result
    }

    private fun updateDistinctDays(state: TrackingState, now: Date): TrackingState {
        val result = state.copy()
        val today = dateFormat.format(now)
        // update distinct days (e.g. in case we've changed timezone or it's already tomorrow
        return result.copy(
            distinctDays = (result.distinctDays + today).distinctBy { it.lowercase() }
        )
    }

    override fun onStop(owner: LifecycleOwner) {
        handleOnBackground()
    }

    fun handleOnBackground(now: Date = Date()) {
        this.manager?.get()?.debugLogger?.debug("Session tracking is shutting down")
        val currentState = this.providerState?.copy(
            lastEnteredBackground = now
        )
        if (currentState != null) {
            this.providerState = currentState
            writeStateToDisk(currentState, this.appContext?.get(), this.fileName, this.fileEncoding, this.manager?.get()?.debugLogger)
        } else {
            this.manager?.get()?.debugLogger?.debug("Session tracking state is empty")
        }
    }

    @Serializable
    data class StoredSession(
        @Serializable(with = DateSerializer::class)
        val firstStart: Date,
        @Serializable(with = DateSerializer::class)
        val ended: Date?,
        // The duration of the session covering the time when the user was actively interacting with the app (based on the app's lifecycle)
        val durationMillis: Long
    )

    @Serializable
    data class TrackingState(
        val sessions: List<StoredSession>,
        // A list of dates on which we have seen the user
        val distinctDays: List<String>,
        val lifetimeSessionsCount: Long? = null,
        @Serializable(with = DateSerializer::class)
        val lastEnteredBackground: Date? = null
    )
}