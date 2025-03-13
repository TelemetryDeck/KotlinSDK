package com.telemetrydeck.sdk

import java.net.URL

internal class TelemetryClientFactory: TelemetryApiClientFactory {
    override fun create(
        apiBaseURL: URL,
        showDebugLogs: Boolean,
        namespace: String?,
        logger: DebugLogger?
    ): TelemetryApiClient {
        return TelemetryClient(
            apiBaseURL,
            showDebugLogs,
            namespace,
            logger
        )
    }
}