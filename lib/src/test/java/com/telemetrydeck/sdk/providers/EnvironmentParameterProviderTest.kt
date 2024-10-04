package com.telemetrydeck.sdk.providers

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.telemetrydeck.sdk.TelemetryDeck
import com.telemetrydeck.sdk.TelemetryManagerConfiguration
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class EnvironmentParameterProviderTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun environmentMetadataProvider_sets_client_version() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val config = TelemetryManagerConfiguration(appID)
        val manager = TelemetryDeck.Builder().configuration(config).build(null)

        manager.signal("type", "clientUser", emptyMap())

        val queuedSignal = manager.cache?.empty()?.first()

        Assert.assertNotNull(queuedSignal)
        Assert.assertEquals(
            true,
            queuedSignal?.payload?.contains("TelemetryDeck.SDK.version:com.telemetrydeck.sdk")

        )
    }

    @Test
    fun environmentMetadataProvider_allows_properties_to_be_set_in_advance() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val config = TelemetryManagerConfiguration(appID)
        val manager = TelemetryDeck.Builder().configuration(config).build(null)

        manager.signal("type", "clientUser", mapOf("telemetryClientVersion" to "my value"))

        val queuedSignal = manager.cache?.empty()?.first()

        Assert.assertNotNull(queuedSignal)
        Assert.assertEquals(
            true,
            queuedSignal?.payload?.contains("telemetryClientVersion:my value")
        )
    }

    @Test
    fun environmentMetadataProvider_sets_sdk_build_type() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val config = TelemetryManagerConfiguration(appID)
        val manager = TelemetryDeck.Builder().configuration(config).build(null)

        manager.signal("type", "clientUser", emptyMap())

        val queuedSignal = manager.cache?.empty()?.first()

        Assert.assertNotNull(queuedSignal)
        Assert.assertEquals(
            true,
            queuedSignal?.payload?.filter { it.startsWith("TelemetryDeck.SDK.buildType:") }?.isNotEmpty()
        )
    }
}