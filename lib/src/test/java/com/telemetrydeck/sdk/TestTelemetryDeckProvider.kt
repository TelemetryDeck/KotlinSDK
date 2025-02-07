package com.telemetrydeck.sdk

import android.content.Context

open class TestTelemetryDeckProvider : TelemetryDeckProvider {
    var registered = false
    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        registered = true
    }

    override fun stop() {
        //
    }
}