package com.telemetrydeck.sdk

import android.app.Application

open class TestTelemetryDeckProvider : TelemetryDeckProvider {
    var registered = false
    override fun register(ctx: Application?, client: TelemetryDeckClient) {
        registered = true
    }

    override fun stop() {
        //
    }
}