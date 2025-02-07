package com.telemetrydeck.sdk.providers


import com.telemetrydeck.sdk.SignalTransform
import org.junit.Assert
import org.junit.Test

class DefaultPrefixProviderTest {

    private fun exampleSignal(type: String = "signal", param: String = "key1"): SignalTransform {
        return SignalTransform(type, "user1", mapOf(param to "value1"), 2.0)
    }

    @Test
    fun default_prefix_provider_no_effect_when_both_prefix_null() {
        val signal = exampleSignal()
        val sut = DefaultPrefixProvider(null, null)
        val result = sut.transform(signal)
        Assert.assertEquals(signal, result)
    }

    @Test
    fun default_prefix_provider_signal_prefix() {
        val signal = exampleSignal()
        val sut = DefaultPrefixProvider("Signal.", null)
        val result = sut.transform(signal)
        val expected = exampleSignal("Signal.signal")
        Assert.assertEquals(expected, result)
    }

    @Test
    fun default_prefix_provider_param_prefix() {
        val signal = exampleSignal()
        val sut = DefaultPrefixProvider(null, "Param.")
        val result = sut.transform(signal)
        val expected = exampleSignal(param = "Param.key1")
        Assert.assertEquals(expected, result)
    }

    @Test
    fun default_prefix_provider_signal_and_param_prefix() {
        val signal = exampleSignal()
        val sut = DefaultPrefixProvider("Signal.", "Param.")
        val result = sut.transform(signal)
        val expected = exampleSignal(type = "Signal.signal", param = "Param.key1")
        Assert.assertEquals(expected, result)
    }

    @Test
    fun default_prefix_provider_ignores_telemetrydeck_signals() {
        val signal = exampleSignal("TelemetryDeck.SDK.name")
        val sut = DefaultPrefixProvider("Signal.", "Param.")
        val result = sut.transform(signal)
        val expected = exampleSignal(type = "TelemetryDeck.SDK.name", param = "Param.key1")
        Assert.assertEquals(expected, result)
    }

    @Test
    fun default_prefix_provider_ignores_telemetrydeck_params() {
        val signal = exampleSignal(param = "TelemetryDeck.something")
        val sut = DefaultPrefixProvider("Signal.", "Param.")
        val result = sut.transform(signal)
        val expected = exampleSignal(type = "Signal.signal", param = "TelemetryDeck.something")
        Assert.assertEquals(expected, result)
    }
}