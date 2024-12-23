package com.telemetrydeck.sdk

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.net.URL
import java.util.Calendar
import java.util.Date
import java.util.UUID
import kotlin.math.abs

class TelemetryManagerTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun telemetryManager_sets_signal_properties() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val config = TelemetryManagerConfiguration(appID)
        val manager = TelemetryManager.Builder().configuration(config).build(null)

        manager.queue("type", "clientUser", emptyMap())

        val queuedSignal = manager.cache?.empty()?.first()

        Assert.assertNotNull(queuedSignal)
        Assert.assertEquals(UUID.fromString(appID), queuedSignal!!.appID)
        Assert.assertEquals(config.sessionID, UUID.fromString(queuedSignal.sessionID))
        Assert.assertEquals("type", queuedSignal.type)
        Assert.assertEquals(
            "6721870580401922549fe8fdb09a064dba5b8792fa018d3bd9ffa90fe37a0149",
            queuedSignal.clientUser
        )
        Assert.assertEquals("false", queuedSignal.isTestMode)
    }

    @Test
    fun telemetryManager_applies_custom_salt() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val config = TelemetryManagerConfiguration(appID)
        config.salt = "my salt"
        val manager = TelemetryManager.Builder().configuration(config).build(null)
        manager.queue("type", "clientUser", emptyMap())
        val queuedSignal = manager.cache?.empty()?.first()
        Assert.assertEquals(
            "9a68a3790deb1db66f80855b8e7c5a97df8002ef90d3039f9e16c94cfbd11d99",
            queuedSignal?.clientUser
        )
    }

    @Test
    fun telemetryManager_builder_set_configuration() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val config = TelemetryManagerConfiguration(appID)
        config.defaultUser = "user"
        config.salt = "salt"

        val sut = TelemetryManager.Builder()

        val result = sut.configuration(config).build(null)

        Assert.assertEquals(UUID.fromString(appID), result.configuration.telemetryAppID)
        Assert.assertEquals(URL("https://nom.telemetrydeck.com"), result.configuration.apiBaseURL)
        Assert.assertEquals("user", result.configuration.defaultUser)
        Assert.assertEquals(config.sessionID, result.configuration.sessionID)
        Assert.assertEquals(config.showDebugLogs, result.configuration.showDebugLogs)
        Assert.assertEquals(config.testMode, result.configuration.testMode)
        Assert.assertEquals(config.salt, result.configuration.salt)
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
    fun telemetryManager_builder_set_salt() {
        val sut = TelemetryManager.Builder()
        val result =
            sut.appID("32CB6574-6732-4238-879F-582FEBEB6536")
                .salt("salty")
                .build(null)
        Assert.assertEquals("salty", result.configuration.salt)
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
    fun telemetryManager_newDefaultUser_changes_defaultUser() {
        val builder = TelemetryManager.Builder()
        val sut = builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .defaultUser("user1")
            .build(null)
        sut.newDefaultUser("user2")
        Assert.assertEquals("user2", sut.configuration.defaultUser)
    }

    @Test
    fun telemetryManager_testMode_on_added_to_signals() {
        val builder = TelemetryManager.Builder()
        val sut = builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .testMode(true)
            .build(null)
        sut.queue("type")

        Assert.assertEquals("true", sut.cache?.empty()?.get(0)?.isTestMode)
    }

    @Test
    fun telemetryManager_testMode_off_added_to_signals() {
        val builder = TelemetryManager.Builder()
        val sut = builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .testMode(false)
            .build(null)
        sut.queue("type")

        Assert.assertEquals("false", sut.cache?.empty()?.get(0)?.isTestMode)
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
        builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .addProvider(provider)
            .build(null)

        Assert.assertTrue(provider.registered)
    }

    @Test
    fun telemetryBroadcastTimer_can_filter_older_signals() {
        // an old signal is received longer than 24h ago
        val okSignal = Signal(appID = UUID.randomUUID(), "okSignal", "user", SignalPayload())
        val oldSignal = Signal(appID = UUID.randomUUID(), "oldSignal", "user", SignalPayload())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -2)
        oldSignal.receivedAt = calendar.time

        val filteredSignals = filterOldSignals(listOf(okSignal, oldSignal))

        Assert.assertEquals(1, filteredSignals.count())
        Assert.assertEquals("okSignal", filteredSignals[0].type)
    }

    @Test
    fun telemetryManager_navigate_source_destination_sets_default_parameters() {
        val config = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536")
        val manager = TelemetryManager.Builder().configuration(config).build(null)

        manager.navigate("source", "destination")

        val queuedSignal = manager.cache?.empty()?.first()

        Assert.assertNotNull(queuedSignal)

        // validate the signal type
        Assert.assertEquals(queuedSignal?.type, "TelemetryDeck.Navigation.pathChanged")

        // validate the navigation status payload
        // https://github.com/TelemetryDeck/KotlinSDK/issues/28
        Assert.assertEquals(
            queuedSignal?.payload?.single { it.startsWith("TelemetryDeck.Navigation.schemaVersion") },
            "TelemetryDeck.Navigation.schemaVersion:1"
        )
        Assert.assertEquals(
            queuedSignal?.payload?.single { it.startsWith("TelemetryDeck.Navigation.identifier") },
            "TelemetryDeck.Navigation.identifier:source -> destination"
        )
        Assert.assertEquals(
            queuedSignal?.payload?.single { it.startsWith("TelemetryDeck.Navigation.sourcePath") },
            "TelemetryDeck.Navigation.sourcePath:source"
        )
        Assert.assertEquals(
            queuedSignal?.payload?.single { it.startsWith("TelemetryDeck.Navigation.destinationPath") },
            "TelemetryDeck.Navigation.destinationPath:destination"
        )
    }

    @Test
    fun telemetryManager_navigate_source_destination_sets_clientUser() {
        val config = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536")
        config.defaultUser = "user"
        val manager = TelemetryManager.Builder().configuration(config).build(null)

        manager.navigate("source", "destination", "clientUser")

        val queuedSignal = manager.cache?.empty()?.first()

        Assert.assertNotNull(queuedSignal)

        // validate that the provided user was used and not default
        Assert.assertEquals(
            queuedSignal?.clientUser,
            "6721870580401922549fe8fdb09a064dba5b8792fa018d3bd9ffa90fe37a0149"
        )
    }

    @Test
    fun telemetryManager_navigate_source_destination_uses_default_user() {
        val config = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536")
        config.defaultUser = "clientUser"
        val manager = TelemetryManager.Builder().configuration(config).build(null)

        manager.navigate("source", "destination")

        val queuedSignal = manager.cache?.empty()?.first()

        Assert.assertNotNull(queuedSignal)

        // validate that the default user was used
        Assert.assertEquals(
            queuedSignal?.clientUser,
            "6721870580401922549fe8fdb09a064dba5b8792fa018d3bd9ffa90fe37a0149"
        )
    }

    @Test
    fun telemetryManager_navigate_destination_no_previous_source() {
        val config = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536")
        val manager = TelemetryManager.Builder().configuration(config).build(null)

        manager.navigate("destination")

        val queuedSignal = manager.cache?.empty()?.first()

        Assert.assertNotNull(queuedSignal)

        // validate the signal type
        Assert.assertEquals(queuedSignal?.type, "TelemetryDeck.Navigation.pathChanged")

        // validate the navigation status payload
        // https://github.com/TelemetryDeck/KotlinSDK/issues/28
        Assert.assertEquals(
            queuedSignal?.payload?.single { it.startsWith("TelemetryDeck.Navigation.schemaVersion") },
            "TelemetryDeck.Navigation.schemaVersion:1"
        )
        Assert.assertEquals(
            queuedSignal?.payload?.single { it.startsWith("TelemetryDeck.Navigation.identifier") },
            "TelemetryDeck.Navigation.identifier: -> destination"
        )
        Assert.assertEquals(
            queuedSignal?.payload?.single { it.startsWith("TelemetryDeck.Navigation.sourcePath") },
            "TelemetryDeck.Navigation.sourcePath:"
        )
        Assert.assertEquals(
            queuedSignal?.payload?.single { it.startsWith("TelemetryDeck.Navigation.destinationPath") },
            "TelemetryDeck.Navigation.destinationPath:destination"
        )
    }

    @Test
    fun telemetryManager_navigate_destination_uses_previous_destination_as_source() {
        val config = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536")
        val manager = TelemetryManager.Builder().configuration(config).build(null)

        manager.navigate("destination1")
        manager.navigate("destination2")

        val queuedSignal = manager.cache?.empty()?.last()

        Assert.assertNotNull(queuedSignal)

        // validate the signal type
        Assert.assertEquals(queuedSignal?.type, "TelemetryDeck.Navigation.pathChanged")

        // validate the navigation status payload
        // https://github.com/TelemetryDeck/KotlinSDK/issues/28
        Assert.assertEquals(
            queuedSignal?.payload?.single { it.startsWith("TelemetryDeck.Navigation.schemaVersion") },
            "TelemetryDeck.Navigation.schemaVersion:1"
        )
        Assert.assertEquals(
            queuedSignal?.payload?.single { it.startsWith("TelemetryDeck.Navigation.identifier") },
            "TelemetryDeck.Navigation.identifier:destination1 -> destination2"
        )
        Assert.assertEquals(
            queuedSignal?.payload?.single { it.startsWith("TelemetryDeck.Navigation.sourcePath") },
            "TelemetryDeck.Navigation.sourcePath:destination1"
        )
        Assert.assertEquals(
            queuedSignal?.payload?.single { it.startsWith("TelemetryDeck.Navigation.destinationPath") },
            "TelemetryDeck.Navigation.destinationPath:destination2"
        )
    }

    private fun filterOldSignals(signals: List<Signal>): List<Signal> {
        val now = Date().time
        return signals.filter {
            // ignore signals older than 24h
            (abs(now - it.receivedAt.time) / 1000) <= 24 * 60 * 60
        }
    }
}

