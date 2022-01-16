package com.telemetrydeck.sdk

import org.junit.Assert
import org.junit.Test
import java.net.URL
import java.util.*

class TelemetryManagerTest {
    @Test
    fun telemetryManager_sets_signal_properties() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val config = TelemetryManagerConfiguration(appID)
        val manager = TelemetryManager(config)

        manager.queue("type", "clientUser", emptyMap())

        val queuedSignal = manager.signalQueue.first()

        Assert.assertNotNull(queuedSignal)
        Assert.assertEquals(UUID.fromString(appID), queuedSignal.appID)
        Assert.assertEquals(config.sessionID, UUID.fromString(queuedSignal.sessionID))
        Assert.assertEquals("type", queuedSignal.type)
        Assert.assertEquals("clientUser", queuedSignal.clientUser)
    }

    @Test
    fun telemetryManager_builder_set_configuration() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val config = TelemetryManagerConfiguration(appID)
        config.defaultUser = "user"

        val sut = TelemetryManager.Builder()

        val result = sut.configuration(config).build(null)

        Assert.assertEquals(UUID.fromString(appID), result.configuration.telemetryAppID)
        Assert.assertEquals(URL("https://nom.telemetrydeck.com"), result.configuration.apiBaseURL)
        Assert.assertEquals("user", result.configuration.defaultUser)
        Assert.assertEquals(config.sessionID, result.configuration.sessionID)
        Assert.assertEquals(config.showDebugLogs, result.configuration.showDebugLogs)
        Assert.assertEquals(config.testMode, result.configuration.testMode)
    }

    @Test
    fun telemetryManager_builder_set_app_ID() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val sut = TelemetryManager.Builder()

        val result = sut.appID(appID).build(null)

        Assert.assertEquals(UUID.fromString(appID), result.configuration.telemetryAppID)
        Assert.assertEquals(URL("https://nom.telemetrydeck.com"), result.configuration.apiBaseURL)
        Assert.assertEquals(null, result.configuration.defaultUser)
    }

    @Test
    fun telemetryManager_builder_set_baseURL() {
        val sut = TelemetryManager.Builder()
        val result =
            sut.appID("32CB6574-6732-4238-879F-582FEBEB6536").baseURL("https://telemetrydeck.com")
                .build(null)
        Assert.assertEquals(URL("https://telemetrydeck.com"), result.configuration.apiBaseURL)
    }


    @Test
    fun telemetryManager_builder_set_testMode() {
        val sut = TelemetryManager.Builder()
        val result = sut
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .testMode(true)
            .build(null)
        Assert.assertEquals(true, result.configuration.testMode)
    }

    @Test
    fun telemetryManager_builder_testMode_off_by_default() {
        val sut = TelemetryManager.Builder()
        val result = sut
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .build(null)
        Assert.assertEquals(false, result.configuration.testMode)
    }

    @Test
    fun telemetryManager_builder_set_defaultUser() {
        val sut = TelemetryManager.Builder()
        val result =
            sut.appID("32CB6574-6732-4238-879F-582FEBEB6536")
                .defaultUser("Dear Person")
                .build(null)
        Assert.assertEquals("Dear Person", result.configuration.defaultUser)
    }

    @Test
    fun telemetryManager_builder_set_showDebugLogs() {
        val sut = TelemetryManager.Builder()
        val result =
            sut
                .appID("32CB6574-6732-4238-879F-582FEBEB6536")
                .showDebugLogs(true)
                .build(null)
        Assert.assertEquals(true, result.configuration.showDebugLogs)
    }

    @Test
    fun telemetryManager_builder_installs_default_logger_with_logging_disabled() {
        val sut = TelemetryManager.Builder()
        val result = sut
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .build(null)
        Assert.assertNotNull(result.logger)
        Assert.assertFalse(result.configuration.showDebugLogs)
    }
}