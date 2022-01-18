package com.telemetrydeck.sdk

import android.app.Application
import android.content.Context
import java.lang.ref.WeakReference
import java.net.URL
import java.util.*


class TelemetryManager(
    val configuration: TelemetryManagerConfiguration,
    val providers: List<TelemetryProvider> = listOf(
        AppLifecycleTelemetryProvider()
    )
) : TelemetryManagerSignals {

    var signalQueue: MutableList<Signal> = mutableListOf()
    var logger: DebugLogger? = null

    override fun queue(
        signalType: String,
        clientUser: String?,
        additionalPayload: Map<String, String>
    ) {
        signalQueue.add(createSignal(signalType, clientUser, additionalPayload))
    }

    override fun queue(
        signalType: SignalType,
        clientUser: String?,
        additionalPayload: Map<String, String>
    ) {
        queue(signalType.type, clientUser, additionalPayload)
    }

    override suspend fun send(
        signalType: String,
        clientUser: String?,
        additionalPayload: Map<String, String>
    ) {
        send(createSignal(signalType, clientUser, additionalPayload))
    }

    override suspend fun send(
        signalType: SignalType,
        clientUser: String?,
        additionalPayload: Map<String, String>
    ) {
        send(signalType.type, clientUser, additionalPayload)
    }

    suspend fun send(
        signal: Signal
    ) {
        send(listOf(signal))
    }

    suspend fun send(
        signals: List<Signal>
    ) {
        val client = TelemetryClient(configuration.telemetryAppID, configuration.apiBaseURL, configuration.showDebugLogs, logger)
        client.send(signals)
    }

    internal var broadcastTimer: TelemetryBroadcastTimer? = null

    private fun installProviders(context: Context?) {
        for (provider in providers) {
            logger?.debug("Installing provider ${provider::class}.")
            provider.register(context?.applicationContext as Application?, this)
        }
    }

    private fun createSignal(
        signalType: String,
        clientUser: String? = null,
        additionalPayload: Map<String, String> = emptyMap()
    ): Signal {
        var enrichedPayload = additionalPayload
        for (provider in this.providers) {
            enrichedPayload = provider.enrich(signalType, clientUser, enrichedPayload)
        }
        val payload = SignalPayload(additionalPayload = enrichedPayload)
        val signal = Signal(
            configuration.telemetryAppID,
            signalType,
            clientUser ?: configuration.defaultUser ?: "",
            payload
        )
        signal.sessionID = this.configuration.sessionID.toString()
        logger?.debug("Created a signal ${signal.type}")
        return signal
    }


    companion object : TelemetryManagerSignals {
        internal val defaultTelemetryProviders: List<TelemetryProvider>
            get() = listOf(
                AppLifecycleTelemetryProvider(),
                EnvironmentMetadataProvider()
            )

        // TelemetryManager singleton
        @Volatile
        private var instance: TelemetryManager? = null

        /**
         * Builds and starts the application instance of `TelemetryManager`.
         * Calling this method multiple times has no effect.
         */
        fun start(context: Application, builder: Builder): TelemetryManager {
            val knownInstance = instance
            if (knownInstance != null) {
                return knownInstance
            }

            return synchronized(this) {
                val syncedInstance = instance
                if (syncedInstance != null) {
                    syncedInstance
                } else {
                    val newInstance = builder.build(context)
                    instance = newInstance
                    newInstance
                }
            }
        }

        /**
         * Shuts down the current instance of `TelemetryManager`.
         */
        fun stop() {
            val manager = getInstance()
                ?: // nothing to do
                return
            manager.broadcastTimer?.stop()
            for (provider in manager.providers) {
                provider.stop()
            }
            synchronized(this) {
                instance = null
            }
        }

        fun getInstance(): TelemetryManager? {
            val knownInstance = instance
            if (knownInstance != null) {
                return knownInstance
            }
            return null
        }

        override fun queue(
            signalType: String,
            clientUser: String?,
            additionalPayload: Map<String, String>
        ) {
            getInstance()?.queue(signalType, clientUser, additionalPayload)
        }

        override fun queue(
            signalType: SignalType,
            clientUser: String?,
            additionalPayload: Map<String, String>
        ) {
            getInstance()?.queue(signalType, clientUser, additionalPayload)
        }

        override suspend fun send(
            signalType: String,
            clientUser: String?,
            additionalPayload: Map<String, String>
        ) {
            getInstance()?.send(signalType, clientUser, additionalPayload)
        }

        override suspend fun send(
            signalType: SignalType,
            clientUser: String?,
            additionalPayload: Map<String, String>
        ) {
            getInstance()?.send(signalType, clientUser, additionalPayload)
        }
    }


    data class Builder(
        private var configuration: TelemetryManagerConfiguration? = null,
        private var providers: List<TelemetryProvider>? = null,
        private var additionalProviders: MutableList<TelemetryProvider>? = null,
        private var appID: UUID? = null,
        private var defaultUser: String? = null,
        private var sessionID: UUID? = null,
        private var testMode: Boolean? = null,
        private var showDebugLogs: Boolean? = null,
        private var apiBaseURL: URL? = null,
        private var logger: DebugLogger? = null
    ) {
        fun configuration(config: TelemetryManagerConfiguration) = apply {
            this.configuration = config
        }

        /**
         * Override the default set of TelemetryProviders
         */
        fun providers(providerList: List<TelemetryProvider>) =
            apply { this.providers = providerList }

        /**
         * Append a custom TelemetryProvider which can produce or enrich signals
         */
        fun addProvider(provider: TelemetryProvider) {
            if (additionalProviders == null) {
                additionalProviders = mutableListOf()
            }
            additionalProviders?.add(provider)
        }

        fun appID(id: String) = apply {
            appID(UUID.fromString(id))
        }

        fun appID(id: UUID) = apply {
            appID = id
        }

        fun baseURL(url: URL) = apply {
            apiBaseURL = url
        }

        fun baseURL(url: String) = apply {
            apiBaseURL = URL(url)
        }

        fun defaultUser(user: String) = apply {
            this.defaultUser = user
        }

        fun sessionID(sessionID: UUID) = apply {
            this.sessionID = sessionID
        }

        fun testMode(testMode: Boolean) = apply {
            this.testMode = testMode
        }

        fun showDebugLogs(showDebugLogs: Boolean) = apply {
            this.showDebugLogs = showDebugLogs
        }

        fun logger(debugLogger: DebugLogger?) = apply {
            this.logger = debugLogger
        }

        fun build(context: Application?): TelemetryManager {
            var config = this.configuration
            val appID = this.appID

            // check if configuration is already set or create a new instance using appID
            if (config == null) {
                if (appID == null) {
                    throw Exception("AppID must be set.")
                }
                config = TelemetryManagerConfiguration(appID)
            }

            // check if providers have been provided or use a default list
            var providers = this.providers
            if (providers == null) {
                providers = TelemetryManager.defaultTelemetryProviders
            }
            // check for additional providers that should be appended
            if (additionalProviders != null) {
                providers = providers + (additionalProviders?.toList() ?: listOf())
            }

            // check if sessionID has been provided to override the default one
            val sessionID = this.sessionID
            if (sessionID != null) {
                config.sessionID = sessionID
            }

            // optional fields
            val defaultUser = this.defaultUser
            if (defaultUser != null) {
                config.defaultUser = defaultUser
            }

            val testMode = this.testMode
            if (testMode != null) {
                config.testMode = testMode
            }

            val showDebugLogs = this.showDebugLogs
            if (showDebugLogs != null) {
                config.showDebugLogs = showDebugLogs
            }

            val logger: DebugLogger = this.logger ?: TelemetryManagerDebugLogger
            logger.configure(config.showDebugLogs)

            val apiBaseURL = this.apiBaseURL
            if (apiBaseURL != null) {
                config.apiBaseURL = apiBaseURL
            }



            val manager = TelemetryManager(config, providers)
            manager.logger = logger
            manager.installProviders(context)
            val broadcaster = TelemetryBroadcastTimer(WeakReference(manager), WeakReference(manager.logger))
            broadcaster.start()
            manager.broadcastTimer = broadcaster
            return manager
        }
    }
}