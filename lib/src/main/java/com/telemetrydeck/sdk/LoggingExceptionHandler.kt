package com.telemetrydeck.sdk

import kotlinx.coroutines.CoroutineExceptionHandler

internal fun loggingExceptionHandler(logger: () -> DebugLogger?): CoroutineExceptionHandler =
    CoroutineExceptionHandler { _, e ->
        logger()?.error("Unhandled error in TelemetryDeck: $e ${e.stackTraceToString()}")
    }
