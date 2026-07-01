package com.telemetrydeck.sdk.providers

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.telemetrydeck.sdk.TelemetryDeckSessionManagerProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.signals.Session
import java.lang.ref.WeakReference
import java.util.UUID

/**
 * [MemorySessionProvider] tracks the current session in memory without any disk I/O.
 */
class MemorySessionProvider : TelemetryDeckSessionManagerProvider, DefaultLifecycleObserver {
    private var manager: WeakReference<TelemetryDeckSignalProcessor>? = null
    private var sessionID: UUID? = null
    private var requestedFirstSessionID: UUID? = null

    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        this.manager = WeakReference(client)
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
        return additionalPayload
    }

    @Synchronized
    override fun onStart(owner: LifecycleOwner) {
        handleOnForeground()
    }

    fun handleOnForeground() {
        if (sessionID != null) return
        val id = requestedFirstSessionID ?: UUID.randomUUID()
        requestedFirstSessionID = null
        startSession(id)
    }

    private fun startSession(id: UUID) {
        sessionID = id
        if (manager?.get()?.configuration?.sendNewSessionBeganSignal == true) {
            manager?.get()?.processSignal(Session.Started.signalName)
        }
    }

    @Synchronized
    override fun getCurrentSessionID(): UUID? = sessionID

    @Synchronized
    override fun startNewSession(sessionID: UUID) {
        this.sessionID = sessionID
    }

    @Synchronized
    override fun setFirstSessionID(sessionID: UUID) {
        check(this.sessionID == null) { "Session tracking is already running" }
        requestedFirstSessionID = sessionID
    }
}
