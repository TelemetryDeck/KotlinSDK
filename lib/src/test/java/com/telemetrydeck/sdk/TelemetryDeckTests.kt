package com.telemetrydeck.sdk

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.telemetrydeck.sdk.providers.DefaultPrefixProvider
import com.telemetrydeck.sdk.providers.DefaultParameterProvider
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

class TelemetryDeckTests {


    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun telemetryDeck_sets_signal_properties() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val config = TelemetryManagerConfiguration(appID)
        val manager = TelemetryDeck.Builder().configuration(config).build(null)

        manager.signal("type", "clientUser", emptyMap())

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
    fun telemetryDeck_applies_custom_salt() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val config = TelemetryManagerConfiguration(appID)
        config.salt = "my salt"
        val manager = TelemetryDeck.Builder().configuration(config).build(null)
        manager.signal("type", "clientUser", emptyMap())
        val queuedSignal = manager.cache?.empty()?.first()
        Assert.assertEquals(
            "9a68a3790deb1db66f80855b8e7c5a97df8002ef90d3039f9e16c94cfbd11d99",
            queuedSignal?.clientUser
        )
    }

    @Test
    fun telemetryDeck_builder_set_configuration() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val config = TelemetryManagerConfiguration(appID)
        config.defaultUser = "user"
        config.salt = "salt"

        val sut = TelemetryDeck.Builder()

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
    fun telemetryDeck_builder_set_app_ID() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val sut = TelemetryDeck.Builder()

        val result = sut.appID(appID).build(null)

        Assert.assertEquals(UUID.fromString(appID), result.configuration.telemetryAppID)
        Assert.assertEquals(URL("https://nom.telemetrydeck.com"), result.configuration.apiBaseURL)
        Assert.assertEquals(null, result.configuration.defaultUser)
    }

    @Test
    fun telemetryDeck_builder_set_baseURL_From_String() {
        val sut = TelemetryDeck.Builder()
        val result =
            sut.appID("32CB6574-6732-4238-879F-582FEBEB6536")
                .baseURL("https://telemetrydeck.com")
                .build(null)
        Assert.assertEquals(URL("https://telemetrydeck.com"), result.configuration.apiBaseURL)
    }

    @Test
    fun telemetryDeck_builder_set_baseURL_FromUrl() {
        val sut = TelemetryDeck.Builder()
        val result =
            sut.appID("32CB6574-6732-4238-879F-582FEBEB6536")
                .baseURL(URL("https://telemetrydeck.com"))
                .build(null)
        Assert.assertEquals(URL("https://telemetrydeck.com"), result.configuration.apiBaseURL)
    }

    @Test
    fun telemetryDeck_builder_set_testMode() {
        val sut = TelemetryDeck.Builder()
        val result = sut
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .testMode(true)
            .build(null)
        Assert.assertEquals(true, result.configuration.testMode)
    }

    @Test
    fun telemetryDeck_builder_testMode_off_by_default() {
        val sut = TelemetryDeck.Builder()
        val result = sut
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .build(null)
        Assert.assertEquals(false, result.configuration.testMode)
    }

    @Test
    fun telemetryDeck_builder_set_defaultUser() {
        val sut = TelemetryDeck.Builder()
        val result =
            sut.appID("32CB6574-6732-4238-879F-582FEBEB6536")
                .defaultUser("Dear Person")
                .build(null)
        Assert.assertEquals("Dear Person", result.configuration.defaultUser)
    }

    @Test
    fun telemetryDeck_builder_set_salt() {
        val sut = TelemetryDeck.Builder()
        val result =
            sut.appID("32CB6574-6732-4238-879F-582FEBEB6536")
                .salt("salty")
                .build(null)
        Assert.assertEquals("salty", result.configuration.salt)
    }

    @Test
    fun telemetryDeck_builder_set_showDebugLogs() {
        val sut = TelemetryDeck.Builder()
        val result =
            sut
                .appID("32CB6574-6732-4238-879F-582FEBEB6536")
                .showDebugLogs(true)
                .build(null)
        Assert.assertEquals(true, result.configuration.showDebugLogs)
    }

    @Test
    fun telemetryDeck_builder_installs_default_logger_with_logging_disabled() {
        val sut = TelemetryDeck.Builder()
        val result = sut
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .build(null)
        Assert.assertNotNull(result.logger)
        Assert.assertFalse(result.configuration.showDebugLogs)
    }

    @Test
    fun telemetryDeck_builder_set_sessionID() {
        val sessionID = UUID.randomUUID()
        val sut = TelemetryDeck.Builder()
        val result = sut
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .sessionID(sessionID)
            .build(null)
        Assert.assertEquals(sessionID, result.configuration.sessionID)
    }

    @Test
    fun telemetryDeck_newSession_resets_sessionID() {
        val sessionID = UUID.randomUUID()
        val builder = TelemetryDeck.Builder()
        val sut = builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .sessionID(sessionID)
            .build(null)
        sut.newSession()
        Assert.assertNotEquals(sessionID, sut.configuration.sessionID)
    }

    @Test
    fun telemetryDeck_newSession_set_preferred_sessionID() {
        val sessionID = UUID.randomUUID()
        val wantedSessionID = UUID.randomUUID()
        Assert.assertNotEquals(sessionID, wantedSessionID)
        val builder = TelemetryDeck.Builder()
        val sut = builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .sessionID(sessionID)
            .build(null)
        sut.newSession(wantedSessionID)
        Assert.assertEquals(wantedSessionID, sut.configuration.sessionID)
    }

    @Test
    fun telemetryDeck_newDefaultUser_changes_defaultUser() {
        val builder = TelemetryDeck.Builder()
        val sut = builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .defaultUser("user1")
            .build(null)
        sut.newDefaultUser("user2")
        Assert.assertEquals("user2", sut.configuration.defaultUser)
    }

    @Test
    fun telemetryDeck_testMode_on_added_to_signals() {
        val builder = TelemetryDeck.Builder()
        val sut = builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .testMode(true)
            .build(null)
        sut.signal("type")

        Assert.assertEquals("true", sut.cache?.empty()?.get(0)?.isTestMode)
    }

    @Test
    fun telemetryDeck_testMode_off_added_to_signals() {
        val builder = TelemetryDeck.Builder()
        val sut = builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .testMode(false)
            .build(null)
        sut.signal("type")

        Assert.assertEquals("false", sut.cache?.empty()?.get(0)?.isTestMode)
    }

    @Test
    fun telemetryDeck_addProvider_appends_after_default_providers() {
        val builder = TelemetryDeck.Builder()
        val sut = builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .addProvider(TestTelemetryDeckProvider())
            .build(null)
        sut.signal("type")

        Assert.assertEquals(5 + 1, sut.providers.count()) // default ones + the one added in the test
        Assert.assertTrue(sut.providers.last() is TestTelemetryDeckProvider)
    }

    @Test
    fun telemetryDeck_addProvider_custom_provider_is_registered() {
        val provider = TestTelemetryDeckProvider()
        Assert.assertFalse(provider.registered)

        val builder = TelemetryDeck.Builder()
        builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .addProvider(provider)
            .build(null)

        Assert.assertTrue(provider.registered)
    }

    @Test
    fun telemetryDeck_navigate_source_destination_sets_default_parameters() {
        val config = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536")
        val manager = TelemetryDeck.Builder().configuration(config).build(null)

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
    fun telemetryDeck_navigate_source_destination_sets_clientUser() {
        val config = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536")
        config.defaultUser = "user"
        val manager = TelemetryDeck.Builder().configuration(config).build(null)

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
    fun telemetryDeck_navigate_source_destination_uses_default_user() {
        val config = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536")
        config.defaultUser = "clientUser"
        val manager = TelemetryDeck.Builder().configuration(config).build(null)

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
    fun telemetryDeck_navigate_destination_no_previous_source() {
        val config = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536")
        val manager = TelemetryDeck.Builder().configuration(config).build(null)

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
    fun telemetryDeck_navigate_destination_uses_previous_destination_as_source() {
        val config = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536")
        val manager = TelemetryDeck.Builder().configuration(config).build(null)

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

    @Test
    fun telemetryDeck_signal_with_floatValue() {
        val config = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536")
        val manager = TelemetryDeck.Builder().configuration(config).build(null)

        manager.signal("test", floatValue = 1.0)

        val queuedSignal = manager.cache?.empty()?.first()

        Assert.assertNotNull(queuedSignal)

        // validate the signal type
        Assert.assertEquals(queuedSignal?.type, "test")

        Assert.assertEquals(queuedSignal?.floatValue, 1.0)
    }

    @Test
    fun telemetryDeck_identityProvider_uses_custom_provider() {
        val builder = TelemetryDeck.Builder()
        val telemetryDeck = builder
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .identityProvider(TestIdentityProvider())
            .build(null)

        telemetryDeck.signal("echo")

        val queuedSignal = telemetryDeck.cache?.empty()?.first()
        Assert.assertEquals(hashString("always the same"), queuedSignal?.clientUser)
    }

    @Test
    fun telemetryDeck_supports_default_parameter_provider() {
        val config = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536")
        val manager = TelemetryDeck.Builder().addProvider(DefaultParameterProvider(mapOf("param1" to "value1"))).configuration(config).build(null)

        manager.signal("test")

        val queuedSignal = manager.cache?.empty()?.first()

        Assert.assertNotNull(queuedSignal)

        // validate the signal type
        Assert.assertEquals(queuedSignal?.type, "test")

        Assert.assertEquals("param1:value1", queuedSignal?.payload?.firstOrNull { it.startsWith("param1:") }, )
    }

    @Test
    fun telemetryDeck_default_prefix_provider() {
        val config = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536")
        val manager = TelemetryDeck.Builder().addProvider(DefaultPrefixProvider("SignalPrefix.", "ParamPrefix.")).configuration(config).build(null)

        manager.signal("test", mapOf("param1" to "value1"), floatValue = 1.0)

        val queuedSignal = manager.cache?.empty()?.first()

        Assert.assertNotNull(queuedSignal)

        // validate the signal type
        Assert.assertEquals("SignalPrefix.test", queuedSignal?.type)
        Assert.assertEquals(1.0, queuedSignal?.floatValue)
        Assert.assertEquals("ParamPrefix.param1:value1", queuedSignal?.payload?.firstOrNull { it.startsWith("ParamPrefix.") }, )
    }

    @Test
    fun telemetryDeck_allows_duration_signal_tracking() {
        val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
        val config = TelemetryManagerConfiguration(appID)
        val manager = TelemetryDeck.Builder().configuration(config).build(null)

        manager.startDurationSignal("type")
        manager.stopAndSendDurationSignal("type")

        val queuedSignal = manager.cache?.empty()?.first()

        Assert.assertNotNull(queuedSignal)
        Assert.assertEquals(UUID.fromString(appID), queuedSignal!!.appID)
        Assert.assertEquals(config.sessionID, UUID.fromString(queuedSignal.sessionID))
        val duration = queuedSignal.payload.find {  it.startsWith("TelemetryDeck.Signal.durationInSeconds:") }
        Assert.assertNotNull(duration)
    }

    private fun hashString(input: String, algorithm: String = "SHA-256"): String {
        return MessageDigest.getInstance(algorithm)
            .digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
}

class TestIdentityProvider: TelemetryDeckIdentityProvider {
    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
        // nothing to do
    }

    override fun stop() {
        // nothing to do
    }

    override fun calculateIdentity(
        signalClientUser: String?,
        configurationDefaultUser: String?
    ): String {
        return "always the same"
    }

    override fun resetIdentity() {
        // nothing to do
    }
}