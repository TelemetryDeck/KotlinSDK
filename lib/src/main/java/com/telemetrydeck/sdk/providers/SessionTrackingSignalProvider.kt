package com.telemetrydeck.sdk.providers

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.telemetrydeck.sdk.DateSerializer
import com.telemetrydeck.sdk.TelemetryDeckSessionManagerProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.TelemetryStorage
import com.telemetrydeck.sdk.UUIDOptionalSerializer
import com.telemetrydeck.sdk.params.Retention
import com.telemetrydeck.sdk.readState
import com.telemetrydeck.sdk.signals.Acquisition
import com.telemetrydeck.sdk.signals.Session
import com.telemetrydeck.sdk.writeState
import kotlinx.serialization.Serializable
import java.lang.ref.WeakReference
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
*
* Monitors the app's lifecycle in order to provide Retention and Acquisition parameters.
*
* This provider broadcasts the following additional signals:
* - `TelemetryDeck.Acquisition.newInstallDetected`
* - `TelemetryDeck.Session.started` (when `sendNewSessionBeganSignal` is set to `true`)
*
*/
class SessionTrackingSignalProvider: TelemetryDeckSessionManagerProvider, DefaultLifecycleObserver {
    private var storage: TelemetryStorage? = null
    private var manager: WeakReference<TelemetryDeckSignalProcessor>? = null
    private var providerState: TrackingState? = null
    private var requestedSessionID: UUID? = null
    private val fileName = "telemetrydeckstracking"
    private val dayStringPattern = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")

    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        this.manager = WeakReference(client)
        this.storage = client.storage
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @Synchronized
    private fun restoreStateIfNeeded() {
        val currentState = this.providerState
        if (currentState == null) {
            // restore offline state
            val restoredState = storage?.readState<TrackingState>(fileName, manager?.get()?.debugLogger)
            var newState = restoredState ?: TrackingState(emptyList(), emptyList())
            newState = repairOldSessions(newState)
            this.providerState = newState
            storage?.writeState(fileName, newState)
        }
    }

    override fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    override fun enrich(
        signalType: String,
        clientUser: String?,
        additionalPayload: Map<String, String>
    ): Map<String, String> {
        return enrichInternal(signalType, clientUser, additionalPayload, Clock.System.now())
    }

    internal fun enrichInternal(
        signalType: String,
        clientUser: String?,
        additionalPayload: Map<String, String>,
        now: Instant
    ): Map<String, String> {
        val signalPayload = additionalPayload.toMutableMap()
        for (item in createMetadata(now)) {
            if (!signalPayload.containsKey(item.key)) {
                signalPayload[item.key] = item.value
            }
        }
        return signalPayload
    }

    private fun createMetadata(now: Instant): Map<String, String> {
        val currentState = this.providerState?.copy() ?: return emptyMap()

        val thirtyDaysAgo = now - 30.days
        val daysLastMonth = currentState.distinctDays.filter {
            val dayStart = parseDayStart(it) ?: return@filter false
            dayStart > thirtyDaysAgo && dayStart < now
        }

        val attributes = mutableMapOf(
            com.telemetrydeck.sdk.params.Acquisition.FirstSessionDate.paramName to (currentState.distinctDays.firstOrNull() ?: ""),
            Retention.DistinctDaysUsed.paramName to "${currentState.distinctDays.size}",
            Retention.DistinctDaysUsedLastMonth.paramName to "${daysLastMonth.size}",
            Retention.TotalSessionsCount.paramName to "${currentState.lifetimeSessionsCount ?: 0}",
        )

        if (currentState.sessions.isNotEmpty()) {
            val completedSessions = currentState.sessions.filter { it.ended != null }
            if (completedSessions.isNotEmpty()) {
                attributes[Retention.AverageSessionSeconds.paramName] =
                    "${completedSessions.map { it.durationMillis / 1000 }.average().toInt()}"

                val lastCompletedSession = completedSessions.lastOrNull {it.ended != null }
                if (lastCompletedSession != null) {
                    attributes[Retention.PreviousSessionSeconds.paramName] = "%.3f".format(lastCompletedSession.durationMillis / 1000.0)
                }
            }
        }

        return attributes
    }

    @Synchronized
    override fun onStart(owner: LifecycleOwner) {
        handleOnForeground()
    }

    fun handleOnForeground(now: Instant = Clock.System.now()) {
        this.manager?.get()?.debugLogger?.debug("Starting session tracking")
        restoreStateIfNeeded()
        var currentState = this.providerState?.copy()
            ?: throw IllegalStateException("Session tracking is not running yet")

        this.manager?.get()?.debugLogger?.debug("Session tracking restoring sessions")
        currentState = updateDistinctDays(currentState, now)

        val newInstall = currentState.sessions.isEmpty()
        val newSessionID = requestedSessionID
        if (newSessionID != null && currentState.sessions.none { it.ended == null && it.id == newSessionID }) {
            // a new session has been explicitly requested
            startSession(newSessionID, now)
        } else {
            if (shouldStartNewSession(currentState, now)) {
                startSession(UUID.randomUUID(), now)
            }
        }
        // stop future sessions from trying to use this ID
        requestedSessionID = null

        // with a running session, we can send new install signal
        if (newInstall) {
            // we're running for the first time
            this.manager?.get()?.debugLogger?.debug("Session tracking is running for the first time")
            val today = dayString(now)
            manager?.get()?.processSignal(
                Acquisition.NewInstallDetected.signalName,
                mapOf(
                    com.telemetrydeck.sdk.params.Acquisition.FirstSessionDate.paramName to today
                )
            )
        }
    }

    /**
     * Returns `true` if more than 5 minutes have expired since the app was sent in the background
     * */
    private fun shouldStartNewSession(state: TrackingState, now: Instant): Boolean {
        val lastEnteredBackground = state.lastEnteredBackground
        if (lastEnteredBackground != null) {
            val backgroundDuration = now - lastEnteredBackground
            return backgroundDuration > 5.minutes
        } else {
            return true
        }
    }

    private fun startNewSessionAndTruncateStorage(state: TrackingState, now: Instant, sessionID: UUID): TrackingState {
        val newSession = StoredSession(sessionID, now, null, 0)
        var result = state.copy()
        val lifetimeSessionsCount = (result.lifetimeSessionsCount ?: 0) + 1
        result = result.copy(
            sessions = result.sessions + newSession,
            lifetimeSessionsCount = lifetimeSessionsCount,
            distinctDays = (result.distinctDays + dayString(now)).distinctBy { it.lowercase() }
        )

        // truncate started more than 90 days ago
        val cutOff = now - 90.days
        val survivingSessions = result.sessions.filter { it.firstStart > cutOff }

        result = result.copy(
            sessions = survivingSessions
        )

        return result
    }

    private fun endLastSessionIfPresent(state: TrackingState, now: Instant): TrackingState {
        val lastSession = state.sessions.lastOrNull()
            ?: // nothing to do
            return state

        var result = state.copy()
        // mark the session as ended and calculate the duration
        this.manager?.get()?.debugLogger?.debug("Session tracking finalizing current session")

        val durationMillis = (now - lastSession.firstStart).inWholeMilliseconds
        result = result.copy(
            sessions = result.sessions.dropLast(1) + lastSession.copy(ended = now, durationMillis = durationMillis),
        )
        return result
    }

    private fun updateDistinctDays(state: TrackingState, now: Instant): TrackingState {
        val result = state.copy()
        val today = dayString(now)
        // update distinct days (e.g. in case we've changed timezone or it's already tomorrow
        return result.copy(
            distinctDays = (result.distinctDays + today).distinctBy { it.lowercase() }
        )
    }

    private fun dayString(instant: Instant): String {
        val calendar = GregorianCalendar()
        calendar.timeInMillis = instant.toEpochMilliseconds()
        val year = calendar.get(Calendar.YEAR).toString().padStart(4, '0')
        val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$year-$month-$day"
    }

    private fun parseDayStart(value: String): Instant? {
        val match = dayStringPattern.matchEntire(value) ?: return null
        val (year, month, day) = match.destructured
        return try {
            val calendar = GregorianCalendar()
            calendar.clear()
            calendar.set(year.toInt(), month.toInt() - 1, day.toInt())
            Instant.fromEpochMilliseconds(calendar.timeInMillis)
        } catch (e: Exception) {
            null
        }
    }

    private fun repairOldSessions(state: TrackingState): TrackingState {
        // repair missing sessionID if session was started without one
        return state.copy(
            sessions = state.sessions.map {
                if (it.ended == null && it.id == null) {
                    it.copy(id = UUID.randomUUID())
                } else {
                    it
                }
            }
        )
    }

    @Synchronized
    override fun onStop(owner: LifecycleOwner) {
        handleOnBackground()
    }

    fun handleOnBackground(now: Instant = Clock.System.now()) {
        this.manager?.get()?.debugLogger?.debug("Session tracking is shutting down")
        val currentState = this.providerState?.copy(
            lastEnteredBackground = now
        )
        if (currentState != null) {
            this.providerState = currentState
            storage?.writeState(fileName, currentState)
        } else {
            this.manager?.get()?.debugLogger?.debug("Session tracking state is empty")
        }
    }

    @Serializable
    data class StoredSession(
        // Identifier of the session
        @Serializable(with = UUIDOptionalSerializer::class)
        val id: UUID? = null,
        @Serializable(with = DateSerializer::class)
        val firstStart: Instant,
        @Serializable(with = DateSerializer::class)
        val ended: Instant?,
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
        val lastEnteredBackground: Instant? = null
    )

    @Synchronized
    override fun getCurrentSessionID(): UUID? {
        return this.providerState?.sessions?.firstOrNull {it.ended == null}?.id
    }

    @Synchronized
    override fun startNewSession(sessionID: UUID) {
        startSession(sessionID, Clock.System.now())
    }

    private fun startSession(sessionID: UUID, now: Instant) {
        this.manager?.get()?.debugLogger?.debug("Starting a new session $sessionID")
        var currentState = this.providerState?.copy()
            ?: throw IllegalStateException("Session tracking is not running yet")
        currentState = endLastSessionIfPresent(currentState, now)
        currentState = startNewSessionAndTruncateStorage(currentState, now, sessionID)
        storage?.writeState(fileName, currentState)
        this.providerState = currentState
        if (manager?.get()?.configuration?.sendNewSessionBeganSignal == true) {
            manager?.get()?.processSignal(
                Session.Started.signalName
            )
        }
    }

    /**
     * Requests the initial sessionID
     *
     * - Will be used as the sessionID if a new session is started.
     * - If a session is being resumed from state the requested sessionID is not the same as the current sessionID, a new session wil be started with the requested id
     */
    @Synchronized
    override fun setFirstSessionID(sessionID: UUID) {
        if (this.providerState != null) {
            throw IllegalStateException("Session tracking is already running")
        }
        requestedSessionID = sessionID
    }
}