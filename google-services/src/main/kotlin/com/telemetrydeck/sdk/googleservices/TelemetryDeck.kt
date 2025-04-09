package com.telemetrydeck.sdk.googleservices

import com.android.billingclient.api.BillingConfig
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.telemetrydeck.sdk.PurchaseEvent
import com.telemetrydeck.sdk.PurchaseType
import com.telemetrydeck.sdk.TelemetryDeck


/**
 * Tracks the completion of a purchase and sends the associated telemetry data.
 *
 * @param billingConfig Google Play Billing Configuration details for the billing process.
 * @param purchase The Google Play Billing purchase object containing details about the completed transaction.
 * @param productDetails Google Play Billing Details about the product that was purchased.
 * @param purchaseOrigin The origin event of the purchase. Defaults to a paid purchase. Use server-side validation to calculate this value. TelemetryDeck provides a simple helper [toTelemetryDeckPurchaseEvent] to try to determine the value locally.
 * @param params A map of additional parameters to include with the telemetry data.
 * @param customUserID An optional custom user identifier.
 *
 *
 * * You can obtain billing configuration from the Google Play Billing library:
 *
 * ```kotlin
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
 *                 // keep billingConfig around until the purchase is completed:
 *                 TelemetryDeck.purchaseCompleted(billingConfig = billingConfig, purchase, productDetails)
 *             }
 *         }
 * })
 *
 * ```
 */
fun TelemetryDeck.Companion.purchaseCompleted(
    billingConfig: BillingConfig,
    purchase: Purchase,
    productDetails: ProductDetails,
    purchaseOrigin: PurchaseEvent = PurchaseEvent.PAID_PURCHASE,
    params: Map<String, String> = emptyMap(),
    customUserID: String? = null
) {
    getInstance()?.purchaseCompleted(
        billingConfig = billingConfig,
        purchase = purchase,
        productDetails = productDetails,
        purchaseOrigin = purchaseOrigin,
        params = params,
        customUserID = customUserID
    )
}

internal fun TelemetryDeck.purchaseCompleted(
    billingConfig: BillingConfig,
    purchase: Purchase,
    productDetails: ProductDetails,
    purchaseOrigin: PurchaseEvent,
    params: Map<String, String>,
    customUserID: String?
) {
    val productId = purchase.products.firstOrNull() ?: ""
    val countryCode = billingConfig.countryCode

    val isSubscription = productDetails.subscriptionOfferDetails != null
    val purchaseType =
        if (isSubscription) PurchaseType.SUBSCRIPTION else PurchaseType.ONE_TIME_PURCHASE
    val oneTimeOffer = productDetails.oneTimePurchaseOfferDetails

    when (purchaseType) {
        PurchaseType.SUBSCRIPTION -> {
            // subscription
            val pricePhase = productDetails.subscriptionOfferDetails
                ?.firstOrNull()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.firstOrNull()
            val priceAmountMicros = pricePhase?.priceAmountMicros ?: 0L
            val currencyCode = pricePhase?.priceCurrencyCode ?: "USD"
            val offerId = productDetails.subscriptionOfferDetails
                ?.firstOrNull()
                ?.offerId
            TelemetryDeck.purchaseCompleted(
                event = purchaseOrigin,
                countryCode = countryCode,
                productID = productId,
                purchaseType = purchaseType,
                priceAmountMicros = priceAmountMicros,
                currencyCode = currencyCode,
                offerID = offerId,
                params = params,
                customUserID = customUserID
            )
        }

        PurchaseType.ONE_TIME_PURCHASE -> {
            // one time purchase
            val priceAmountMicros = oneTimeOffer?.priceAmountMicros ?: 0L
            val currencyCode = oneTimeOffer?.priceCurrencyCode ?: "USD"
            TelemetryDeck.purchaseCompleted(
                event = purchaseOrigin,
                countryCode = countryCode,
                productID = productId,
                purchaseType = purchaseType,
                priceAmountMicros = priceAmountMicros,
                currencyCode = currencyCode,
                params = params,
                customUserID = customUserID
            )
        }
    }
}

/**
 * Converts a `Purchase` object into a corresponding `PurchaseEvent` based on the purchase's SKU
 * and its trial or paid purchase status.
 *
 * This method attempts to guess if a purchase corresponds to a trial conversion by checking the locally available information.
 * We check if:
 * 1) If the product was part of a known trial SKU and
 * 2) If the purchase was recent (within the default trial window).
 *
 *
 * - This does not detect free trials used on another device/account.
 * - You must manually maintain the list of trial SKUs or base plans locally.
 * - Does not work for Play offers that use multiple offer tokens under the same product ID.
 *
 *
 * * It is best to implement server-side validation in order to query the Google Play API and inspect the paymentState.
 *
 * @param knownTrialSkus a set of SKUs that are recognized as free trial SKUs. List of product IDs (or SKUs) that you know include free trials. This should be manually maintained based on how you set up offers in the Play Console.
 * @param trialWindowMs the duration (in milliseconds) considered as the trial period, defaulting to 7 days.
 * @return a `PurchaseEvent` indicating the type of purchase: either a free trial start, trial conversion, or a standard paid purchase.
 */
fun Purchase.toTelemetryDeckPurchaseEvent(
    knownTrialSkus: Set<String>,
    trialWindowMs: Long = 7 * 24 * 60 * 60 * 1000L // 7 days
): PurchaseEvent {
    val sku = products.firstOrNull() ?: return PurchaseEvent.PAID_PURCHASE

    return when {
        // If SKU is one of our known free trial SKUs and the purchase is recent
        sku in knownTrialSkus && isWithinTrialWindow(purchaseTime, trialWindowMs) -> {
            PurchaseEvent.STARTED_FREE_TRIAL
        }

        // If SKU is a known trial SKU but purchase is outside trial window
        sku in knownTrialSkus && !isWithinTrialWindow(purchaseTime, trialWindowMs) -> {
            PurchaseEvent.CONVERTED_FROM_TRIAL
        }

        // Otherwise it's a standard paid purchase
        else -> PurchaseEvent.PAID_PURCHASE
    }
}

private fun isWithinTrialWindow(purchaseTimeMillis: Long, trialWindowMs: Long): Boolean {
    val currentTime = System.currentTimeMillis()
    return currentTime - purchaseTimeMillis <= trialWindowMs
}
