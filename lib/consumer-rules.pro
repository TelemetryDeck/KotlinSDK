# TelemetryDeck KotlinSDK — consumer ProGuard rules
#
# Intentionally minimal. R8 / ProGuard correctly preserves what the SDK needs
# because:
#   - kotlinx.serialization, Ktor, AndroidX Lifecycle and kotlinx.coroutines
#     ship their own consumer-rules.pro inside their AARs.
#   - The SDK's public API (TelemetryDeck, Builder, providers, Signal,
#     TelemetryManagerConfiguration, …) is referenced by consumer code,
#     so R8's data-flow analysis preserves it.
#   - TelemetryDeckInitProvider is declared in AndroidManifest.xml; the
#     manifest merger keeps it automatically.
#   - The only runtime `is` type check (DurationSignalTrackerProvider) is
#     obfuscation-safe: both sides obfuscate to the same name.
#
# If you hit an R8 issue caused by this SDK, please open an issue at
# https://github.com/TelemetryDeck/KotlinSDK/issues with the stack trace
# and your R8 configuration.
