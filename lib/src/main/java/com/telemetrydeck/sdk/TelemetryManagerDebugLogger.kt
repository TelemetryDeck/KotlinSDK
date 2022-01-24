package com.telemetrydeck.sdk

/**
 * An implementation of `DebugLogger` using `android.util.Log`
 */
internal class TelemetryManagerDebugLogger {

    companion object: DebugLogger {
        private const val tag: String = "TELEMETRYDECK"
        private var enabled: Boolean = true
        override fun error(message: String) {
            if (enabled) {
                println("E/$tag: $message")
            }

        }

        override fun debug(message: String) {
            if (enabled) {
                println("D/$tag: $message")
            }
        }

        override fun configure(enabled: Boolean) {
            this.enabled = enabled
        }
    }
}