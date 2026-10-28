package com.telemetrydeck.sdk

import android.content.Context

class FailingTelemetryDeckProvider(
    private val failInRegister: Boolean = false,
    private val failInEnrich: Boolean = false,
    private val failInTransform: Boolean = false,
) : TestTelemetryDeckProvider() {
    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        if (failInRegister) throw IllegalStateException("register failure")
        super.register(ctx, client)
    }

    override fun enrich(
        signalType: String,
        clientUser: String?,
        additionalPayload: Map<String, String>
    ): Map<String, String> {
        if (failInEnrich) throw IllegalStateException("enrich failure")
        return additionalPayload + (PARAM_NAME to "value")
    }

    override fun transform(signalTransform: SignalTransform): SignalTransform {
        if (failInTransform) throw IllegalStateException("transform failure")
        return signalTransform
    }

    companion object {
        const val PARAM_NAME = "FailingProvider.param"
    }
}
