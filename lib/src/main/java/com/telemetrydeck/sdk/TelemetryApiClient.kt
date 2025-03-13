package com.telemetrydeck.sdk

import java.net.URL

interface TelemetryApiClient {
    suspend fun send(
        signals: List<Signal>
    )

    fun getServiceUrl(): URL
}

interface TelemetryApiClientFactory {
    fun create(
        apiBaseURL: URL,
        showDebugLogs: Boolean,
        logger:DebugLogger?
    ): TelemetryApiClient
}