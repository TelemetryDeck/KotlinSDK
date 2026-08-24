package com.telemetrydeck.sdk

import kotlinx.serialization.json.JsonElement

/**
 * Internal counterpart to [TelemetryDeckProvider] for providers that need to attach typed JSON
 * values (numbers, arrays) to a signal's payload instead of strings.
 *
 * [TelemetryDeck] calls this after the string-based [TelemetryDeckProvider.enrich]/[TelemetryDeckProvider.transform]
 * pipeline has run, merging the returned values into the signal's payload without overwriting keys
 * already present.
 */
internal interface TelemetryDeckTypedProvider {
    fun enrichTyped(signalType: String, clientUser: String?): Map<String, JsonElement>
}
