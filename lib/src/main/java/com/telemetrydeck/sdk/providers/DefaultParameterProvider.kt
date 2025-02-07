package com.telemetrydeck.sdk.providers

import android.content.Context
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor


/**
 * [DefaultParameterProvider] is a [TelemetryDeckProvider] that adds default parameters
 * to every telemetry signal. It ensures that signals are enriched with predefined data,
 * unless the signal already contains a value for the same key.
 *
 * @property defaultParameters A map of key-value pairs representing the default parameters
 *                            that will be added to each telemetry signal.
 */
class DefaultParameterProvider(val defaultParameters: Map<String, String>): TelemetryDeckProvider {
    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        // nothing to do
    }

    override fun stop() {
        // nothing to do
    }

    override fun enrich(
        signalType: String,
        clientUser: String?,
        additionalPayload: Map<String, String>
    ): Map<String, String> {
        val signalPayload = additionalPayload.toMutableMap()
        for (item in defaultParameters) {
            if (!signalPayload.containsKey(item.key)) {
                signalPayload[item.key] = item.value
            }
        }
        return signalPayload
    }
}