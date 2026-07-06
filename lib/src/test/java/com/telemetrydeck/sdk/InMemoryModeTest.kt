package com.telemetrydeck.sdk

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.telemetrydeck.sdk.providers.FileUserIdentityProvider
import com.telemetrydeck.sdk.providers.MemorySessionProvider
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class InMemoryModeTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val appID = "32CB6574-6732-4238-879F-582FEBEB6536"
    private val sha256OfEmpty = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

    @Test
    fun inMemoryOnly_uses_MemorySignalCache() {
        val manager = TelemetryDeck.Builder()
            .appID(appID)
            .inMemoryOnly(true)
            .build(null)

        Assert.assertTrue(manager.cache is MemorySignalCache)
    }

    @Test
    fun inMemoryOnly_uses_FileUserIdentityProvider_with_InMemoryStorage() {
        val manager = TelemetryDeck.Builder()
            .appID(appID)
            .inMemoryOnly(true)
            .build(null)

        Assert.assertTrue(manager.identityProvider is FileUserIdentityProvider)
        Assert.assertTrue(manager.storage is InMemoryStorage)
    }

    @Test
    fun inMemoryOnly_uses_MemorySessionProvider() {
        val manager = TelemetryDeck.Builder()
            .appID(appID)
            .inMemoryOnly(true)
            .build(null)

        Assert.assertTrue(manager.sessionManager is MemorySessionProvider)
    }

    @Test
    fun inMemoryOnly_explicit_signalCache_overrides_flag() {
        val customCache = MemorySignalCache(cacheLimit = 5)
        val manager = TelemetryDeck.Builder()
            .appID(appID)
            .inMemoryOnly(true)
            .signalCache(customCache)
            .build(null)

        Assert.assertSame(customCache, manager.cache)
    }

    @Test
    fun inMemoryOnly_explicit_storage_overrides_flag() {
        val customStorage = InMemoryStorage()
        val manager = TelemetryDeck.Builder()
            .appID(appID)
            .inMemoryOnly(true)
            .storage(customStorage)
            .build(null)

        Assert.assertSame(customStorage, manager.storage)
    }

    @Test
    fun inMemoryOnly_explicit_identityProvider_overrides_flag() {
        val customIdentity = object : TelemetryDeckIdentityProvider {
            override fun register(ctx: android.content.Context?, client: TelemetryDeckSignalProcessor) {}
            override fun stop() {}
            override fun calculateIdentity(signalClientUser: String?, configurationDefaultUser: String?) = "custom"
            override fun resetIdentity() {}
        }
        val manager = TelemetryDeck.Builder()
            .appID(appID)
            .inMemoryOnly(true)
            .identityProvider(customIdentity)
            .build(null)

        Assert.assertSame(customIdentity, manager.identityProvider)
    }

    @Test
    fun inMemoryOnly_explicit_sessionProvider_overrides_flag() {
        val customSession = object : TelemetryDeckSessionManagerProvider {
            override fun register(ctx: android.content.Context?, client: TelemetryDeckSignalProcessor) {}
            override fun stop() {}
            override fun getCurrentSessionID(): java.util.UUID? = null
            override fun startNewSession(sessionID: java.util.UUID) {}
            override fun setFirstSessionID(sessionID: java.util.UUID) {}
        }
        val manager = TelemetryDeck.Builder()
            .appID(appID)
            .inMemoryOnly(true)
            .sessionProvider(customSession)
            .build(null)

        Assert.assertSame(customSession, manager.sessionManager)
    }

    @Test
    fun inMemoryOnly_anonymous_user_produces_non_empty_stable_clientUser() {
        val manager = TelemetryDeck.Builder()
            .appID(appID)
            .inMemoryOnly(true)
            .build(null)

        manager.signal("test.event")
        val signal = manager.cache?.empty()?.firstOrNull()

        Assert.assertNotNull(signal)
        Assert.assertFalse(signal!!.clientUser.isNullOrEmpty())
        Assert.assertNotEquals(sha256OfEmpty, signal.clientUser)
    }

    @Test
    fun inMemoryOnly_anonymous_user_is_stable_within_instance() {
        val manager = TelemetryDeck.Builder()
            .appID(appID)
            .inMemoryOnly(true)
            .build(null)

        manager.signal("first.event")
        manager.signal("second.event")
        val signals = manager.cache?.empty() ?: emptyList()

        Assert.assertEquals(2, signals.size)
        Assert.assertEquals(signals[0].clientUser, signals[1].clientUser)
    }

    @Test
    fun inMemoryOnly_anonymous_user_differs_between_instances() {
        val manager1 = TelemetryDeck.Builder().appID(appID).inMemoryOnly(true).build(null)
        val manager2 = TelemetryDeck.Builder().appID(appID).inMemoryOnly(true).build(null)

        manager1.signal("event")
        manager2.signal("event")

        val user1 = manager1.cache?.empty()?.firstOrNull()?.clientUser
        val user2 = manager2.cache?.empty()?.firstOrNull()?.clientUser

        Assert.assertNotNull(user1)
        Assert.assertNotNull(user2)
        Assert.assertNotEquals(user1, user2)
    }

    @Test
    fun nullContext_uses_MemorySignalCache() {
        val manager = TelemetryDeck.Builder()
            .appID(appID)
            .build(null)

        Assert.assertTrue(manager.cache is MemorySignalCache)
    }

    @Test
    fun nullContext_uses_FileUserIdentityProvider_with_InMemoryStorage() {
        val manager = TelemetryDeck.Builder()
            .appID(appID)
            .build(null)

        Assert.assertTrue(manager.identityProvider is FileUserIdentityProvider)
        Assert.assertTrue(manager.storage is InMemoryStorage)
    }

    @Test
    fun nullContext_uses_MemorySessionProvider() {
        val manager = TelemetryDeck.Builder()
            .appID(appID)
            .build(null)

        Assert.assertTrue(manager.sessionManager is MemorySessionProvider)
    }

    @Test
    fun nullContext_anonymous_user_produces_non_empty_stable_clientUser() {
        val manager = TelemetryDeck.Builder()
            .appID(appID)
            .build(null)

        manager.signal("test.event")
        val signal = manager.cache?.empty()?.firstOrNull()

        Assert.assertNotNull(signal)
        Assert.assertFalse(signal!!.clientUser.isNullOrEmpty())
        Assert.assertNotEquals(sha256OfEmpty, signal.clientUser)
    }
}
