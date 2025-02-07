package com.telemetrydeck.sdk.providers

import android.content.Context
import com.telemetrydeck.sdk.SignalTransform
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor


/**
 * [DefaultPrefixProvider] is a [TelemetryDeckProvider] that adds a default prefix to all outgoing signals or parameters.
 *
 * For example, if you are already adding `AppName.` in front of every signal or parameter, just specify it here and no need to repeat over and over again.
 *
 * This provider ignores signals and parameters in the `TelemetryDeck` namespace.
 *
 * @property defaultSignalPrefix Specify this if you want us to prefix all your signals with a specific text.
 * @property defaultParameterPrefix Specify this if you want us to prefix all your signal parameters with a specific text.
 */
class DefaultPrefixProvider(val defaultSignalPrefix: String?, val defaultParameterPrefix: String?) :
    TelemetryDeckProvider {
    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        // nothing to do
    }

    override fun stop() {
        // nothing to do
    }

    override fun transform(signalTransform: SignalTransform): SignalTransform {
        val paramPrefix = defaultParameterPrefix
        val signalPrefix = defaultSignalPrefix

        if (paramPrefix == null && signalPrefix == null) {
            // nothing to do
            return signalTransform
        }

        var transform =
            signalTransform.copy(additionalPayload = signalTransform.additionalPayload.toMutableMap())

        if (paramPrefix != null) {
            transform =
                transform.copy(additionalPayload = transform.additionalPayload
                    .map {
                        if (!it.key.startsWith("TelemetryDeck.")) {
                            ("$paramPrefix${it.key}" to it.value)
                        } else {
                            // do not prefix our own signals
                            (it.key to it.value)
                        }
                    }.toMap())
        }

        if (signalPrefix != null && !transform.signalType.startsWith("TelemetryDeck.")) {
            transform = transform.copy(signalType = "$signalPrefix${transform.signalType}")
        }

        return transform
    }
}