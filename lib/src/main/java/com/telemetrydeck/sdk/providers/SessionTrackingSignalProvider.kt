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
import com.telemetrydeck.sdk.signals.Acquisition
import kotlinx.serialization.Serializable
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
*
* Keeps track of user sessions and provides for TelemetryDeck.Acquisition.firstSessionDate
* */
class SessionTrackingSignalProvider: TelemetryDeckProvider, DefaultLifecycleObserver {
    private var appContext: WeakReference<Context?>? = null
    private var manager: WeakReference<TelemetryDeckSignalProcessor>? = null
    private var state: TrackingState? = null
    private val fileName = "telemetrydeckstracking"
    private val fileEncoding = Charsets.UTF_8
    private val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        this.manager = WeakReference(client)
        this.appContext = WeakReference(ctx)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun stop() {
        // nothing to do
    }

    override fun onStart(owner: LifecycleOwner) {
        handleOnStart()
    }

    fun handleOnStart() {
        this.manager?.get()?.debugLogger?.debug("Commencing session tracking")
        val preRestoreState = this.state
        if (preRestoreState == null) {
            // no state present in memory, restore it
            val restoredState = restoreStateFromDisk<TrackingState?>(
                this.appContext?.get(),
                this.fileName,
                this.fileEncoding,
                this.manager?.get()?.debugLogger
            )
            this.state = restoredState
        }

        val currentState = this.state ?: TrackingState(emptyList(), emptyList())

        this.manager?.get()?.debugLogger?.debug("Session tracking restoring sessions")
        val now = Date()
        val today = dateFormat.format(now)
        if (currentState.sessions.isEmpty()) {
            // we're running for the first time
            this.manager?.get()?.debugLogger?.debug("Session tracking is running for the first time")
            manager?.get()?.processSignal(
                Acquisition.NewInstallDetected.signalName,
                mapOf(
                    com.telemetrydeck.sdk.params.Acquisition.FirstSessionDate.paramName to today
                )
            )
        }

        // start a new session
        val newSession = StoredSession(now, null, 0)
        val updatedState = currentState.copy(
            sessions = currentState.sessions + newSession,
            distinctDays = (currentState.distinctDays + today).distinctBy { it.lowercase() }
        )

        // delete session started more than 90 days ago
        val cutOff = Date(Date().time - (90L * 24L * 60L * 60L * 1000L))
        val survivingSessions = updatedState.sessions.filter { it.firstStart.after(cutOff) }

        val saveState = updatedState.copy(
            sessions = survivingSessions
        )

        this.manager?.get()?.debugLogger?.debug("Session tracking ${saveState.sessions.size} sessions.")
        writeStateToDisk(saveState, this.appContext?.get(), this.fileName, this.fileEncoding, this.manager?.get()?.debugLogger)
        this.state = saveState
    }

    override fun onStop(owner: LifecycleOwner) {
        handleOnStop()
    }

    fun handleOnStop() {
        this.manager?.get()?.debugLogger?.debug("Session tracking is shutting down")
        var currentState = this.state
        if (currentState != null) {
            val lastSession = currentState.sessions.lastOrNull()
            val now = Date()
            val today = dateFormat.format(now)
            if (lastSession != null) {
                // mark the session as ended and calculate the duration
                this.manager?.get()?.debugLogger?.debug("Session tracking finalizing current session")

                val durationMillis = now.time - lastSession.firstStart.time
                currentState = currentState.copy(
                    sessions = currentState.sessions.dropLast(1) + lastSession.copy(ended = now, durationMillis = durationMillis),
                )
            }

            // update distinct days (e.g. in case we've changed timezone or it's already tomorrow
            currentState = currentState.copy(
                distinctDays = (currentState.distinctDays + today).distinctBy { it.lowercase() }
            )

            writeStateToDisk(currentState, this.appContext?.get(), this.fileName, this.fileEncoding, this.manager?.get()?.debugLogger)
        } else {
            this.manager?.get()?.debugLogger?.debug("Session tracking has no current state, nothing to do")
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
    )
}