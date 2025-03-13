package com.telemetrydeck.sdk

import java.net.URL

interface TelemetryApiClientFactory {
    fun create(
        apiBaseURL: URL,
        showDebugLogs: Boolean,
        namespace: String?,
        logger: DebugLogger?
    ): TelemetryApiClient
}