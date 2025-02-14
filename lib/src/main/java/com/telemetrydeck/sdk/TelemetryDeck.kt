package com.telemetrydeck.sdk

import android.content.Context
import android.content.pm.ApplicationInfo
import com.telemetrydeck.sdk.params.Navigation
import com.telemetrydeck.sdk.providers.AccessibilityProvider
import com.telemetrydeck.sdk.providers.DurationSignalTrackerProvider
import com.telemetrydeck.sdk.providers.EnvironmentParameterProvider
import com.telemetrydeck.sdk.providers.FileUserIdentityProvider
import com.telemetrydeck.sdk.providers.PlatformContextProvider
import com.telemetrydeck.sdk.providers.SessionAppProvider
import com.telemetrydeck.sdk.providers.SessionTrackingSignalProvider
import java.lang.ref.WeakReference
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

class TelemetryDeck(
    override val configuration: TelemetryManagerConfiguration,
    val providers: List<TelemetryDeckProvider>
) : TelemetryDeckClient, TelemetryDeckSignalProcessor {
    var cache: SignalCache? = null
    var logger: DebugLogger? = null
    var identityProvider: TelemetryDeckIdentityProvider = FileUserIdentityProvider()
    private val navigationStatus: NavigationStatus = MemoryNavigationStatus()

    override val signalCache: SignalCache?
        get() = this.cache

    override val debugLogger: DebugLogger?
        get() = this.logger

    override fun newSession(sessionID: UUID) {
        this.configuration.sessionID = sessionID
    }

    override fun newDefaultUser(user: String?) {
        this.configuration.defaultUser = user
    }

    override fun navigate(sourcePath: String, destinationPath: String, clientUser: String?) {
        navigationStatus.applyDestination(destinationPath)

        val params: Map<String, String> = mapOf(
            Navigation.SchemaVersion.paramName to "1",
            Navigation.Identifier.paramName to "$sourcePath -> $destinationPath",
            Navigation.SourcePath.paramName to sourcePath,
            Navigation.DestinationPath.paramName to destinationPath
        )

        signal(
            com.telemetrydeck.sdk.signals.Navigation.PathChanged.signalName,
            params = params,
            customUserID = clientUser
        )
    }

    override fun navigate(destinationPath: String, customUserID: String?) {
        navigate(navigationStatus.getLastDestination(), destinationPath, customUserID)
    }

    override suspend fun send(
        signalType: String,
        clientUser: String?,
        additionalPayload: Map<String, String>,
        floatValue: Double?
    ): Result<Unit> {
        return send(createSignal(signalType, clientUser, additionalPayload, floatValue))
    }

    override suspend fun sendAll(signals: List<Signal>): Result<Unit> {
        return send(signals)
    }

    override fun processSignal(
        signalName: String,
        params: Map<String, String>,
        floatValue: Double?,
        customUserID: String?
    ) {
        signal(signalName, params, floatValue, customUserID)
    }

    override fun signal(
        signalName: String,
        params: Map<String, String>,
        floatValue: Double?,
        customUserID: String?
    ) {
        cache?.add(
            createSignal(
                signalType = signalName,
                clientUser = customUserID,
                additionalPayload = params,
                floatValue = floatValue
            )
        )
    }

    override fun resetSession(sessionID: UUID) {
        newSession(sessionID)
    }

    override fun signal(signalName: String, customUserID: String?, params: Map<String, String>) {
        cache?.add(
            createSignal(
                signalType = signalName,
                clientUser = customUserID,
                additionalPayload = params,
                floatValue = null
            )
        )
    }

    override fun startDurationSignal(signalName: String, parameters: Map<String, String>) {
        val trackingProvider = this.providers.find { it is DurationSignalTrackerProvider } as? DurationSignalTrackerProvider
        if (trackingProvider == null) {
            this.logger?.error("startDurationSignal requires the DurationSignalTrackerProvider to be registered")
            return
        }
        trackingProvider.startTracking(signalName, parameters)
    }

    override fun stopAndSendDurationSignal(signalName: String, parameters: Map<String, String>) {
        val trackingProvider = this.providers.find { it is DurationSignalTrackerProvider } as? DurationSignalTrackerProvider
        if (trackingProvider == null) {
            this.logger?.error("stopAndSendDurationSignal requires the DurationSignalTrackerProvider to be registered")
            return
        }
        val params = trackingProvider.stopTracking(signalName, parameters)
        if (params != null) {
            processSignal(signalName, params = params)
        }
    }

    private suspend fun send(
        signal: Signal
    ): Result<Unit> {
        return send(listOf(signal))
    }

    private suspend fun send(
        signals: List<Signal>
    ): Result<Unit> {
        return try {
            val client = TelemetryClient(
                configuration.apiBaseURL,
                configuration.showDebugLogs,
                logger
            )
            client.send(signals)
            success(Unit)
        } catch (e: Exception) {
            logger?.error("Failed to send signals due to an error $e ${e.stackTraceToString()}")
            failure(e)
        }
    }

    internal var broadcastTimer: TelemetryBroadcastTimer? = null

    private fun installProviders(context: Context?) {
        for (provider in providers) {
            logger?.debug("Installing provider ${provider::class}.")
            provider.register(context, this)
        }
        identityProvider.register(context, this)
    }

    private fun createSignal(
        signalType: String,
        clientUser: String? = null,
        additionalPayload: Map<String, String> = emptyMap(),
        floatValue: Double?
    ): Signal {
        var enrichedPayload = additionalPayload
        for (provider in this.providers) {
            enrichedPayload = provider.enrich(signalType, clientUser, enrichedPayload)
        }

        var signalTransform = SignalTransform(signalType, clientUser, enrichedPayload, floatValue)
        for (provider in this.providers) {
            signalTransform = provider.transform(signalTransform)
        }
        return signalFromTransform(signalTransform)
    }

    private fun signalFromTransform(signalTransform: SignalTransform): Signal {
        val userValue = identityProvider.calculateIdentity(signalTransform.clientUser, configuration.defaultUser)

        val userValueWithSalt = userValue + (configuration.salt ?: "")
        val hashedUser = hashString(userValueWithSalt)

        val payload = SignalPayload(additionalPayload = signalTransform.additionalPayload)
        val signal = Signal(
            appID = configuration.telemetryAppID,
            type = signalTransform.signalType,
            clientUser = hashedUser,
            payload = payload.asMultiValueDimension,
            isTestMode = configuration.testMode.toString().lowercase(),
            floatValue = signalTransform.floatValue
        )
        signal.sessionID = this.configuration.sessionID.toString()
        logger?.debug("Created a signal ${signal.type}, session ${signal.sessionID}, test ${signal.isTestMode}")
        return signal
    }

    private fun hashString(input: String, algorithm: String = "SHA-256"): String {
        return MessageDigest.getInstance(algorithm)
            .digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }

    companion object : TelemetryDeckClient, TelemetryDeckSignalProcessor {
        internal val defaultTelemetryProviders: List<TelemetryDeckProvider>
            get() = listOf(
                SessionAppProvider(),
                EnvironmentParameterProvider(),
                PlatformContextProvider(),
                AccessibilityProvider(),
                SessionTrackingSignalProvider()
            )
        internal val alwaysOnProviders = listOf(DurationSignalTrackerProvider())

        // [TelemetryDeck] singleton
        @Volatile
        private var instance: TelemetryDeck? = null

        /**
         * Builds and starts the application instance of [TelemetryDeck].
         * Calling this method multiple times has no effect.
         */
        fun start(context: Context, builder: Builder): TelemetryDeck {
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
         * Shuts down the current instance of [TelemetryDeck].
         */
        fun stop() {
            val manager = getInstance()
                ?: // nothing to do
                return
            manager.broadcastTimer?.stop()
            for (provider in manager.providers) {
                provider.stop()
            }
            manager.identityProvider.stop()
            synchronized(this) {
                instance = null
            }
        }

        private fun getInstance(): TelemetryDeck? {
            val knownInstance = instance
            if (knownInstance != null) {
                return knownInstance
            }
            return null
        }

        override fun newSession(sessionID: UUID) {
            getInstance()?.newSession(sessionID)
        }

        override fun newDefaultUser(user: String?) {
            getInstance()?.newDefaultUser(user)
        }

        override fun navigate(sourcePath: String, destinationPath: String, clientUser: String?) {
            getInstance()?.navigate(sourcePath, destinationPath, clientUser = clientUser)
        }

        override fun navigate(destinationPath: String, customUserID: String?) {
            getInstance()?.navigate(destinationPath, customUserID = customUserID)
        }

        override suspend fun send(
            signalType: String,
            clientUser: String?,
            additionalPayload: Map<String, String>,
            floatValue: Double?
        ): Result<Unit> {
            val result = getInstance()?.send(signalType, clientUser, additionalPayload, floatValue)
            if (result != null) {
                return result
            }
            return failure(NullPointerException())
        }

        override suspend fun sendAll(signals: List<Signal>): Result<Unit> {
            val result = getInstance()?.sendAll(signals)
            if (result != null) {
                return result
            }
            return failure(NullPointerException())
        }

        override fun processSignal(
            signalName: String,
            params: Map<String, String>,
            floatValue: Double?,
            customUserID: String?
        ) {
            signal(signalName, params, floatValue, customUserID)
        }

        override fun resetSession(sessionID: UUID) {
            newSession(sessionID)
        }

        override fun signal(
            signalName: String,
            params: Map<String, String>,
            floatValue: Double?,
            customUserID: String?
        ) {
            getInstance()?.signal(signalName, params, floatValue, customUserID)
        }

        override fun signal(
            signalName: String,
            customUserID: String?,
            params: Map<String, String>
        ) {
            getInstance()?.signal(
                signalName = signalName,
                customUserID = customUserID,
                params = params
            )
        }

        override fun startDurationSignal(signalName: String, parameters: Map<String, String>) {
            getInstance()?.startDurationSignal(signalName, parameters)
        }

        override fun stopAndSendDurationSignal(
            signalName: String,
            parameters: Map<String, String>
        ) {
            getInstance()?.stopAndSendDurationSignal(signalName, parameters)
        }

        override val signalCache: SignalCache?
            get() = getInstance()?.signalCache

        override val debugLogger: DebugLogger?
            get() = getInstance()?.debugLogger

        override val configuration: TelemetryManagerConfiguration?
            get() = getInstance()?.configuration
    }


    data class Builder(
        private var configuration: TelemetryManagerConfiguration? = null,
        private var providers: List<TelemetryDeckProvider>? = null,
        private var additionalProviders: MutableList<TelemetryDeckProvider>? = null,
        private var appID: UUID? = null,
        private var defaultUser: String? = null,
        private var sessionID: UUID? = null,
        private var testMode: Boolean? = null,
        private var showDebugLogs: Boolean? = null,
        private var sendNewSessionBeganSignal: Boolean? = null,
        private var apiBaseURL: URL? = null,
        private var logger: DebugLogger? = null,
        private var salt: String? = null,
        private var identityProvider: TelemetryDeckIdentityProvider? = null
    ) {
        /**
         * Set the [TelemetryDeck] configuration.
         * Use this method to directly set all configuration fields and bypass any default values.
         *
         */
        fun configuration(config: TelemetryManagerConfiguration) = apply {
            this.configuration = config
        }

        /**
         * Override the default set of TelemetryProviders.
         */
        fun providers(providerList: List<TelemetryDeckProvider>) =
            apply { this.providers = providerList }

        /**
         * Append a custom [TelemetryDeckProvider] which can produce or enrich signals
         */
        fun addProvider(provider: TelemetryDeckProvider) = apply {
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

        fun sendNewSessionBeganSignal(sendNewSessionBeganSignal: Boolean) = apply {
            this.sendNewSessionBeganSignal = sendNewSessionBeganSignal
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

        fun salt(salt: String?) = apply {
            this.salt = salt
        }

        fun identityProvider(identityProvider: TelemetryDeckIdentityProvider) = apply {
            this.identityProvider = identityProvider
        }

        /**
         * Provide a custom logger implementation to be used by [TelemetryDeck] when logging internal messages.
         */
        fun logger(debugLogger: DebugLogger?) = apply {
            this.logger = debugLogger
        }

        fun build(context: Context?): TelemetryDeck {
            var config = this.configuration
            val appID = this.appID
            // check if configuration is already set or create a new instance using appID
            val initConfiguration = config == null
            if (config == null) {
                if (appID == null) {
                    throw Exception("AppID must be set.")
                }
                config = TelemetryManagerConfiguration(appID)
            }

            // check if providers have been provided or use a default list
            var providers = this.providers
            if (providers == null) {
                providers = defaultTelemetryProviders
            }
            providers = providers + alwaysOnProviders
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
            } else {
                // do not change testMode if it was provided through a configuration object
                if (initConfiguration) {
                    config.testMode = 0 != (context?.applicationInfo?.flags
                        ?: 0) and ApplicationInfo.FLAG_DEBUGGABLE
                }
            }

            val salt = this.salt
            if (salt != null) {
                config.salt = salt
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

            val sendNewSessionBeganSignal = sendNewSessionBeganSignal
            if (sendNewSessionBeganSignal != null) {
                config.sendNewSessionBeganSignal = sendNewSessionBeganSignal
            }

            val manager = TelemetryDeck(config, providers)
            manager.logger = logger

            val broadcaster =
                TelemetryBroadcastTimer(WeakReference(manager), WeakReference(manager.logger))
            broadcaster.start()
            manager.broadcastTimer = broadcaster

            if (context != null) {
                manager.cache = PersistentSignalCache(context.cacheDir, logger)
            } else {
                manager.cache = MemorySignalCache()
            }

            val userIdentityProvider = this.identityProvider
            if (userIdentityProvider != null) {
                manager.identityProvider = userIdentityProvider
            }

            // providers must be installed at the end to allow them access to cache and full signal processing
            manager.installProviders(context)
            
            return manager
        }
    }
}