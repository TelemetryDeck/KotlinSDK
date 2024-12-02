package com.telemetrydeck.sdk

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class EnvironmentMetadataProviderTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun environmentMetadataProvider_sets_client_version() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val config = TelemetryManagerConfiguration(appID)
        val manager = TelemetryManager.Builder().configuration(config).build(null)

        manager.queue("type", "clientUser", emptyMap())

        val queuedSignal = manager.cache?.empty()?.first()

        Assert.assertNotNull(queuedSignal)
        Assert.assertEquals(
            queuedSignal?.payload?.contains("telemetryClientVersion:com.telemetrydeck.sdk"),
            true
        )
        Assert.assertEquals(
            queuedSignal?.payload?.contains("TelemetryDeck.SDK.version:com.telemetrydeck.sdk"),
            true
        )
    }

    @Test
    fun environmentMetadataProvider_allows_properties_to_be_set_in_advance() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val config = TelemetryManagerConfiguration(appID)
        val manager = TelemetryManager.Builder().configuration(config).build(null)

        manager.queue("type", "clientUser", mapOf("telemetryClientVersion" to "my value"))

        val queuedSignal = manager.cache?.empty()?.first()

        Assert.assertNotNull(queuedSignal)
        Assert.assertEquals(
            queuedSignal?.payload?.contains("telemetryClientVersion:my value"),
            true
        )
    }
}