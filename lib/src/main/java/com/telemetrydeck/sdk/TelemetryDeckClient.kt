package com.telemetrydeck.sdk

import java.util.UUID

interface TelemetryDeckClient {

    /**
     * All future signals belong to a new session.
     *
     * Calling this method sets a new SessionID for new Signals. Previously queued signals are not affected.
     */
    fun newSession(sessionID: UUID = UUID.randomUUID())


    /**
     * Set the default user for future signals
     *
     */
    fun newDefaultUser(user: String?)


    /**
     * Send a signal that represents a navigation event with a source and a destination.
     *
     * @see <a href="https://telemetrydeck.com/docs/articles/navigation-signals/">Navigation Signals</a>
     * */
    fun navigate(sourcePath: String, destinationPath: String, clientUser: String? = null)

    /**
     * Send a signal that represents a navigation event with a destination and a default source.
     *
     * @see <a href="https://telemetrydeck.com/docs/articles/navigation-signals/">Navigation Signals</a>
     * */
    fun navigate(destinationPath: String, customUserID: String? = null)

    /**
     * Send a `TelemetryDeck.Acquisition.userAcquired` signal with the provided channel.
     */
    @ExperimentalFeature
    fun acquiredUser(channel: String, params: Map<String, String> = emptyMap(), customUserID: String? = null)


    /**
     * Send a `TelemetryDeck.Acquisition.leadStarted` signal with the provided leadId.
     */
    @ExperimentalFeature
    fun leadStarted(leadId: String, params: Map<String, String> = emptyMap(), customUserID: String? = null)


    /**
     * Send a `TelemetryDeck.Acquisition.leadConverted` signal with the provided leadId.
     */
    @ExperimentalFeature
    fun leadConverted(leadId: String, params: Map<String, String> = emptyMap(), customUserID: String? = null)


    /**
     * Send a signal immediately
     */
    suspend fun send(
        signalType: String,
        clientUser: String? = null,
        additionalPayload: Map<String, String> = emptyMap(),
        floatValue: Double? = null,
    ): Result<Unit>


    /**
     *  Sends a telemetry signal with optional parameters to TelemetryDeck.
     *
     *
     *  Signals are first queued in cache (see [SignalCache]) before being sent to the server.
     *  In case of failure, we will try sending again approximately every 10 seconds while the app is running.
     *
     *  When running in the context of an application, the signal cache is written to a local file so signals are saved when the app restarts (see [PersistentSignalCache]).
     *  When running without a context, the signal cache is stored in memory. All cached (unsent) signals are discarded when the TelemetryDeck SDK instance has been disposed (see [MemorySignalCache]).
     *
     *
     *  If you prefer to control the lifecycle of signals, use the [send] method instead.
     *
     * @param signalName The name of the signal to be sent. This is a string that identifies the type of event or action being reported.
     * @param params A map of additional string key-value pairs that provide further context about the signal.
     * @param floatValue An optional floating-point number that can be used to provide numerical data about the signal.
     * @param customUserID An optional string specifying a custom user identifier. If provided, it will override the default user identifier from the configuration.
     *
     */
    fun signal(
        signalName: String,
        params: Map<String, String> = emptyMap(),
        floatValue: Double? = null,
        customUserID: String? = null,
    )

    /**
     *  Sends a telemetry signal with optional parameters to TelemetryDeck.
     *
     *
     *  Signals are first queued in cache (see [SignalCache]) before being sent to the server.
     *  In case of failure, we will try sending again approximately every 10 seconds while the app is running.
     *
     *  When running in the context of an application, the signal cache is written to a local file so signals are saved when the app restarts (see [PersistentSignalCache]).
     *  When running without a context, the signal cache is stored in memory. All cached (unsent) signals are discarded when the TelemetryDeck SDK instance has been disposed (see [MemorySignalCache]).
     *
     *
     *  If you prefer to control the lifecycle of signals, use the [send] method instead.
     *
     * @param signalName The name of the signal to be sent. This is a string that identifies the type of event or action being reported.
     * @param customUserID An optional string specifying a custom user identifier. If provided, it will override the default user identifier from the configuration.
     * @param params A map of additional string key-value pairs that provide further context about the signal.
     *
     */
    fun signal(
        signalName: String,
        customUserID: String? = null,
        params: Map<String, String> = emptyMap(),
    )

    /** Starts tracking the duration of a signal without sending it yet.
     *
     * This function only starts tracking time – it does not send a signal. You must call `[stopAndSendDurationSignal]`
     * with the same signal name to finalize and actually send the signal with the tracked duration.
     *
     * Calling this method twice for the same signal name will replace the previous tracking.
     *
     * @param signalName The name of the signal to track. This will be used to identify and stop the duration tracking later.
     * @param parameters A dictionary of additional string key-value pairs that will be included when the duration signal is eventually sent.
     * @param includeBackgroundTime Indicates if the duration tracked will include the time spent in the background.
     */
    fun startDurationSignal(
        signalName: String,
        parameters: Map<String, String> = emptyMap(),
        includeBackgroundTime: Boolean = false
    )

    /** Stops tracking the duration of a signal and sends it with the total duration.
     *
     * This function finalizes the duration tracking by:
     * 1. Stopping the timer for the given signal name
     * 2. Calculating the duration in seconds (excluding background time)
     * 3. Sending a signal that includes the start parameters, stop parameters, and calculated duration
     *
     * Tracked time will be included as a value of `TelemetryDeck.Signal.durationInSeconds`.
     * Tracked time only includes time while the app is in the foreground.
     *
     * Note: If no matching signal was started, this function has no effect.
     *
     * @param signalName The name of the signal that was previously started with [startDurationSignal]
     * @param parameters Additional parameters to include with the signal. These will be merged with the parameters provided at the start.
     * @param floatValue An optional floating-point number that can be used to provide numerical data about the signal.
     * @param customUserID An optional string specifying a custom user identifier. If provided, it will override the default user identifier from the configuration.
     *
     */
    fun stopAndSendDurationSignal(signalName: String, parameters: Map<String, String> = emptyMap(), floatValue: Double? = null, customUserID: String? = null)


    /**
     * Logs the completion of a purchase event.
     *
     *
     * @param event A `PurchaseEvent` instance representing the type of purchase action being tracked. Instances can include purchase completion, free trial start, or conversion from trial.
     * @param countryCode The country code format is based on ISO-3166-1 alpha2 (UN country codes). https://unicode.org/cldr/charts/latest/supplemental/territory_containment_un_m_49.html
     * @param productID The unique identifier of the purchased product.
     * @param purchaseType The type of purchase, either a subscription or a one-time purchase.
     * @param priceAmountMicros The price of the product in micro-units of the currency (e.g., 1,000,000 micro-units equal 1 unit of the currency).
     * @param currencyCode The ISO 4217 currency code (e.g., "EUR" for Euro) used for the purchase.
     * @param offerID: The specific offer identifier for subscription products.
     * @param params A map of additional string key-value pairs that provide further context about the signal.
     * @param customUserID An optional string specifying a custom user identifier. If provided, it will override the default user identifier from the configuration.
     *
     *
     *
     *
     * Once a purchase is completed, you can obtain purchase detail information from the billing library:
     *
     * ```kotlin
     * // For one-time purchases (ProductDetails from Billing Library 5.0+), [PurchaseType.ONE_TIME_PURCHASE]
     * fun getPurchaseDetails(productDetails: ProductDetails) {
     *     // Get one-time purchase offering
     *     val oneTimePurchaseOfferDetails = productDetails.oneTimePurchaseOfferDetails
     *     oneTimePurchaseOfferDetails?.let {
     *         val formattedPrice = it.formattedPrice // e.g., "$1.99"
     *         val priceAmountMicros = it.priceAmountMicros // e.g., 1990000 (for $1.99)
     *         val currencyCode = it.priceCurrencyCode // e.g., "USD"
     *     }
     * }
     *
     * // For subscriptions, [PurchaseType.SUBSCRIPTION]
     * fun getSubscriptionDetails(productDetails: ProductDetails) {
     *     // Get all subscription offers
     *     val subscriptionOfferDetails = productDetails.subscriptionOfferDetails
     *     subscriptionOfferDetails?.forEach { offerDetails ->
     *         // Each offer might have multiple pricing phases
     *         offerDetails.pricingPhases.pricingPhaseList.forEach { pricingPhase ->
     *             val priceAmountMicros = pricingPhase.priceAmountMicros
     *             val currencyCode = pricingPhase.priceCurrencyCode
     *         }
     *     }
     * }
     *
     * // Obtaining the Google Play Country code
     * val getBillingConfigParams = GetBillingConfigParams.newBuilder().build()
     * billingClient.getBillingConfigAsync(getBillingConfigParams,
     *     object : BillingConfigResponseListener {
     *         override fun onBillingConfigResponse(
     *             billingResult: BillingResult,
     *             billingConfig: BillingConfig?
     *         ) {
     *             if (billingResult.responseCode == BillingResponseCode.OK
     *                 && billingConfig != null) {
     *                 val countryCode = billingConfig.countryCode
     *                 ...
     *             }
     *         }
     *     })
     *
     * ```
     */
    fun purchaseCompleted(event: PurchaseEvent, countryCode: String, productID: String, purchaseType: PurchaseType, priceAmountMicros: Long, currencyCode: String, offerID: String? = null, params: Map<String, String> = emptyMap(), customUserID: String? = null)

    val configuration: TelemetryManagerConfiguration?
}