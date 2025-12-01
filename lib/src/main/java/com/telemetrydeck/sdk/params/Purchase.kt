package com.telemetrydeck.sdk.params

enum class Purchase(val paramName: String) {
    Type("TelemetryDeck.Purchase.type"),
    CountryCode("TelemetryDeck.Purchase.countryCode"),
    CurrencyCode("TelemetryDeck.Purchase.currencyCode"),
    ProductID("TelemetryDeck.Purchase.productID"),
    OfferID("TelemetryDeck.Purchase.offerID"),
    PriceMicros("TelemetryDeck.Purchase.priceMicros"),
}