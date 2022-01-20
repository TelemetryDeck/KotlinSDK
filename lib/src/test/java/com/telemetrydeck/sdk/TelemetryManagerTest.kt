package com.telemetrydeck.sdk

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.verify
import java.net.URL
import java.util.*

class TelemetryManagerTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun telemetryManager_sets_signal_properties() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val config = TelemetryManagerConfiguration(appID)
        val manager =  TelemetryManager.Builder().configuration(config).build(null)

        manager.queue("type", "clientUser", emptyMap())

        val queuedSignal = manager.cache?.empty()?.first()

        Assert.assertNotNull(queuedSignal)
        Assert.assertEquals(UUID.fromString(appID), queuedSignal!!.appID)
        Assert.assertEquals(config.sessionID, UUID.fromString(queuedSignal.sessionID))
        Assert.assertEquals("type", queuedSignal.type)
        Assert.assertEquals("clientUser", queuedSignal.clientUser)
        Assert.assertEquals(false, queuedSignal.isTestMode)
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

    @Test
    fun telemetryManager_builder_set_sessionID() {
        val sessionID = UUID.randomUUID()
        val sut = TelemetryManager.Builder()
        val result = sut
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .sessionID(sessionID)
            .build(null)
        Assert.assertEquals(sessionID, result.configuration.sessionID)
    }

    @Test
    fun telemetryManager_newSession_resets_sessionID() {
        val sessionID = UUID.randomUUID()
        val builder = TelemetryManager.Builder()
        val sut = builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .sessionID(sessionID)
            .build(null)
        sut.newSession()
        Assert.assertNotEquals(sessionID, sut.configuration.sessionID)
    }

    @Test
    fun telemetryManager_newSession_set_preferred_sessionID() {
        val sessionID = UUID.randomUUID()
        val wantedSessionID = UUID.randomUUID()
        Assert.assertNotEquals(sessionID, wantedSessionID)
        val builder = TelemetryManager.Builder()
        val sut = builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .sessionID(sessionID)
            .build(null)
        sut.newSession(wantedSessionID)
        Assert.assertEquals(wantedSessionID, sut.configuration.sessionID)
    }

    @Test
    fun telemetryManager_testMode_on_added_to_signals() {
        val builder = TelemetryManager.Builder()
        val sut = builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .testMode(true)
            .build(null)
        sut.queue("type")

        Assert.assertEquals(true, sut.cache?.empty()?.get(0)?.isTestMode)
    }

    @Test
    fun telemetryManager_testMode_off_added_to_signals() {
        val builder = TelemetryManager.Builder()
        val sut = builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .testMode(false)
            .build(null)
        sut.queue("type")

        Assert.assertEquals(false, sut.cache?.empty()?.get(0)?.isTestMode)
    }

    @Test
    fun telemetryManager_addProvider_appends_after_default_providers() {
        val builder = TelemetryManager.Builder()
        val sut = builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .addProvider(TestProvider())
            .build(null)
        sut.queue("type")

        Assert.assertEquals(4, sut.providers.count())
        Assert.assertTrue(sut.providers[3] is TestProvider)
    }

    @Test
    fun telemetryManager_addProvider_custom_provider_is_registered() {
        val provider = TestProvider()
        Assert.assertFalse(provider.registered)

        val builder = TelemetryManager.Builder()
        val sut = builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .addProvider(provider)
            .build(null)

        Assert.assertTrue(provider.registered)
    }
}

open class TestProvider: TelemetryProvider {
    var registered = false
    override fun register(ctx: Application?, manager: TelemetryManager) {
        registered = true
    }

    override fun stop() {
        //
    }
}