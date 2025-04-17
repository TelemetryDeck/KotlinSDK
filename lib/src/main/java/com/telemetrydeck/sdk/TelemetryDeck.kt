package com.telemetrydeck.sdk

import android.content.Context
import android.content.pm.ApplicationInfo
import com.telemetrydeck.sdk.params.Acquisition
import com.telemetrydeck.sdk.params.Navigation
import com.telemetrydeck.sdk.providers.AccessibilityProvider
import com.telemetrydeck.sdk.providers.CalendarParameterProvider
import com.telemetrydeck.sdk.providers.DurationSignalTrackerProvider
import com.telemetrydeck.sdk.providers.EnvironmentParameterProvider
import com.telemetrydeck.sdk.providers.FileUserIdentityProvider
import com.telemetrydeck.sdk.providers.PlatformContextProvider
import com.telemetrydeck.sdk.providers.SessionTrackingSignalProvider
import com.telemetrydeck.sdk.signals.Purchase
import java.lang.ref.WeakReference
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

class TelemetryDeck(
    override val configuration: TelemetryManagerConfiguration,
    val providers: List<TelemetryDeckProvider>,
) : TelemetryDeckClient, TelemetryDeckSignalProcessor {
    var cache: SignalCache? = null
    var logger: DebugLogger? = null
    var sessionManager: TelemetryDeckSessionManagerProvider? = null
    var identityProvider: TelemetryDeckIdentityProvider = FileUserIdentityProvider()
    var telemetryClientFactory: TelemetryApiClientFactory = TelemetryClientFactory()
    private val navigationStatus: NavigationStatus = MemoryNavigationStatus()

    override val signalCache: SignalCache?
        get() = this.cache

    override val debugLogger: DebugLogger?
        get() = this.logger

    override val sessionID: UUID?
        get() {
            return sessionManager?.getCurrentSessionID()
        }

    override fun newSession(sessionID: UUID) {
        sessionManager?.startNewSession(sessionID)
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

    @ExperimentalFeature
    override fun acquiredUser(channel: String, params: Map<String, String>, customUserID: String?) {
        val signalParams = mergeMapsWithOverwrite(params, mapOf(
            Acquisition.Channel.paramName to channel
        ))
        signal(
            com.telemetrydeck.sdk.signals.Acquisition.UserAcquired.signalName,
            params = signalParams,
            customUserID = customUserID
        )
    }

    @ExperimentalFeature
    override fun leadStarted(leadId: String, params: Map<String, String>, customUserID: String?) {
        val signalParams = mergeMapsWithOverwrite(params, mapOf(
            Acquisition.LeadId.paramName to leadId
        ))
        signal(
            com.telemetrydeck.sdk.signals.Acquisition.LeadStarted.signalName,
            params = signalParams,
            customUserID = customUserID
        )
    }

    @ExperimentalFeature
    override fun leadConverted(leadId: String, params: Map<String, String>, customUserID: String?) {
        val signalParams = mergeMapsWithOverwrite(params, mapOf(
            Acquisition.LeadId.paramName to leadId
        ))
        signal(
            com.telemetrydeck.sdk.signals.Acquisition.LeadConverted.signalName,
            params = signalParams,
            customUserID = customUserID
        )
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

    override fun startDurationSignal(signalName: String, parameters: Map<String, String>, includeBackgroundTime: Boolean) {
        val trackingProvider = this.providers.find { it is DurationSignalTrackerProvider } as? DurationSignalTrackerProvider
        if (trackingProvider == null) {
            this.logger?.error("startDurationSignal requires the DurationSignalTrackerProvider to be registered")
            return
        }
        trackingProvider.startTracking(signalName, parameters, includeBackgroundTime)
    }

    override fun stopAndSendDurationSignal(signalName: String, parameters: Map<String, String>, floatValue: Double?, customUserID: String?) {
        val trackingProvider = this.providers.find { it is DurationSignalTrackerProvider } as? DurationSignalTrackerProvider
        if (trackingProvider == null) {
            this.logger?.error("stopAndSendDurationSignal requires the DurationSignalTrackerProvider to be registered")
            return
        }
        val params = trackingProvider.stopTracking(signalName, parameters)
        if (params != null) {
            processSignal(
                signalName,
                params = params,
                floatValue = floatValue,
                customUserID = customUserID
            )
        }
    }

    override fun purchaseCompleted(
        event: PurchaseEvent,
        countryCode: String,
        productID: String,
        purchaseType: PurchaseType,
        priceAmountMicros: Long,
        currencyCode: String,
        offerID: String?,
        params: Map<String, String>,
        customUserID: String?
    ) {

        val purchaseParams = mutableMapOf(
            com.telemetrydeck.sdk.params.Purchase.Type.paramName to when (purchaseType) {
                PurchaseType.SUBSCRIPTION -> "subscription"
                PurchaseType.ONE_TIME_PURCHASE -> "one-time-purchase"
            },
            com.telemetrydeck.sdk.params.Purchase.CountryCode.paramName to countryCode,
            com.telemetrydeck.sdk.params.Purchase.CurrencyCode.paramName to currencyCode,
            com.telemetrydeck.sdk.params.Purchase.ProductID.paramName to productID,
            com.telemetrydeck.sdk.params.Purchase.PriceMicros.paramName to priceAmountMicros.toString()
        )

        if (offerID != null) {
            purchaseParams[com.telemetrydeck.sdk.params.Purchase.OfferID.paramName] = offerID
        }

        val signalParams = mergeMapsWithOverwrite(params, purchaseParams)

        signal(
            when(event) {
                PurchaseEvent.STARTED_FREE_TRIAL -> Purchase.FreeTrialStarted.signalName
                PurchaseEvent.CONVERTED_FROM_TRIAL -> Purchase.ConvertedFromTrial.signalName
                PurchaseEvent.PAID_PURCHASE -> Purchase.Completed.signalName
            },
            params = signalParams,
            floatValue = CurrencyConverter.convertToUSD(priceAmountMicros, currencyCode),
            customUserID = customUserID
        )
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
            val client = telemetryClientFactory.create(
                configuration.apiBaseURL,
                configuration.showDebugLogs,
                configuration.namespace,
                logger,
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
        // session manager must be installed first as some plugins may depend on it
        sessionManager?.register(context, this)
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
        enrichedPayload = sessionManager?.enrich(signalType, clientUser, enrichedPayload) ?: enrichedPayload
        for (provider in this.providers) {
            enrichedPayload = provider.enrich(signalType, clientUser, enrichedPayload)
        }

        var signalTransform = SignalTransform(signalType, clientUser, enrichedPayload, floatValue)
        signalTransform = sessionManager?.transform(signalTransform) ?: signalTransform
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
        signal.sessionID = this.sessionManager?.getCurrentSessionID()?.toString()
        logger?.debug("Created a signal ${signal.type}, session ${signal.sessionID}, test ${signal.isTestMode}")
        return signal
    }

    private fun hashString(input: String, algorithm: String = "SHA-256"): String {
        return MessageDigest.getInstance(algorithm)
            .digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun mergeMapsWithOverwrite(map1: Map<String, String>, map2: Map<String, String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        result.putAll(map1)
        result.putAll(map2)
        return result
    }

    companion object : TelemetryDeckClient, TelemetryDeckSignalProcessor {
        internal val defaultTelemetryProviders: List<TelemetryDeckProvider>
            get() = listOf(
                EnvironmentParameterProvider(),
                PlatformContextProvider(),
                AccessibilityProvider(),
                CalendarParameterProvider()
            )
        internal val alwaysOnProviders = listOf(DurationSignalTrackerProvider())

        // [TelemetryDeck] singleton
        @Volatile
        internal var instance: TelemetryDeck? = null

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
            manager.sessionManager?.stop()
            synchronized(this) {
                instance = null
            }
        }

        /**
         * Retrieves the current instance of the `TelemetryDeck` singleton if available.
         * If no instance exists, this method returns `null`.
         *
         * @return The current `TelemetryDeck` instance, or `null` if no instance is available.
         */
        fun getInstance(): TelemetryDeck? {
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

        @ExperimentalFeature
        override fun acquiredUser(
            channel: String,
            params: Map<String, String>,
            customUserID: String?
        ) {
            getInstance()?.acquiredUser(channel, params, customUserID)
        }

        @ExperimentalFeature
        override fun leadStarted(
            leadId: String,
            params: Map<String, String>,
            customUserID: String?
        ) {
            getInstance()?.leadStarted(leadId, params, customUserID)
        }

        @ExperimentalFeature
        override fun leadConverted(
            leadId: String,
            params: Map<String, String>,
            customUserID: String?
        ) {
            getInstance()?.leadConverted(leadId, params, customUserID)
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

        override fun startDurationSignal(signalName: String, parameters: Map<String, String>, includeBackgroundTime: Boolean) {
            getInstance()?.startDurationSignal(signalName, parameters, includeBackgroundTime)
        }

        override fun stopAndSendDurationSignal(
            signalName: String,
            parameters: Map<String, String>,
            floatValue: Double?,
            customUserID: String?
        ) {
            getInstance()?.stopAndSendDurationSignal(signalName, parameters, floatValue, customUserID)
        }

        override fun purchaseCompleted(
            event: PurchaseEvent,
            countryCode: String,
            productID: String,
            purchaseType: PurchaseType,
            priceAmountMicros: Long,
            currencyCode: String,
            offerID: String?,
            params: Map<String, String>,
            customUserID: String?
        ) {
            getInstance()?.purchaseCompleted(
                event,
                countryCode,
                productID,
                purchaseType,
                priceAmountMicros,
                currencyCode,
                offerID,
                params,
                customUserID
            )
        }

        override val signalCache: SignalCache?
            get() = getInstance()?.signalCache

        override val debugLogger: DebugLogger?
            get() = getInstance()?.debugLogger
        override val sessionID: UUID?
            get() = getInstance()?.sessionID

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
        private var identityProvider: TelemetryDeckIdentityProvider? = null,
        private var sessionProvider: TelemetryDeckSessionManagerProvider? = null,
        private var telemetryClientFactory: TelemetryApiClientFactory? = null,
        private var signalCache: SignalCache? = null,
        private var namespace: String? = null,
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

        fun sessionProvider(sessionManagerProvider: TelemetryDeckSessionManagerProvider?) = apply {
            this.sessionProvider = sessionManagerProvider
        }

        fun apiClientFactory(factory: TelemetryApiClientFactory) = apply {
            this.telemetryClientFactory = factory
        }

        fun signalCache(cache: SignalCache) = apply {
            this.signalCache = cache
        }

        /**
         * Provide a custom logger implementation to be used by [TelemetryDeck] when logging internal messages.
         */
        fun logger(debugLogger: DebugLogger?) = apply {
            this.logger = debugLogger
        }

        fun namespace(namespace: String?) = apply {
            this.namespace = namespace
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
            val namespace = this.namespace
            if (!namespace.isNullOrBlank()) {
                config.namespace = namespace
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

            if (telemetryClientFactory != null) {
                manager.telemetryClientFactory = telemetryClientFactory as TelemetryApiClientFactory
            }

            val broadcaster =
                TelemetryBroadcastTimer(WeakReference(manager), WeakReference(manager.logger))
            manager.broadcastTimer = broadcaster

            if (signalCache != null) {
                // user-specified cache implementation
                manager.cache = signalCache
            } else {
                // determine based on context availability
                if (context != null) {
                    manager.cache = PersistentSignalCache(context.cacheDir, logger)
                } else {
                    manager.cache = MemorySignalCache()
                }
            }

            val userIdentityProvider = this.identityProvider
            if (userIdentityProvider != null) {
                manager.identityProvider = userIdentityProvider
            }

            val customSessionProvider = this.sessionProvider
            manager.sessionManager = customSessionProvider ?: SessionTrackingSignalProvider()
            val sessionID = this.sessionID
            if (sessionID != null) {
                manager.sessionManager?.setFirstSessionID(sessionID)
            }

            // providers must be installed at the end to allow them access to cache and full signal processing
            manager.installProviders(context)

            // start signal broadcast
            broadcaster.start()
            
            return manager
        }
    }
}