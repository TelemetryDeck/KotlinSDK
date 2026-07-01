package com.telemetrydeck.sdk.providers

import org.junit.Assert
import org.junit.Test

class MemoryUserIdentityProviderTest {

    @Test
    fun calculateIdentity_returns_signalClientUser_when_present() {
        val sut = MemoryUserIdentityProvider()
        val result = sut.calculateIdentity("signal-user", "config-user")
        Assert.assertEquals("signal-user", result)
    }

    @Test
    fun calculateIdentity_returns_configurationDefaultUser_when_no_signal_user() {
        val sut = MemoryUserIdentityProvider()
        val result = sut.calculateIdentity(null, "config-user")
        Assert.assertEquals("config-user", result)
    }

    @Test
    fun calculateIdentity_returns_sessionIdentity_when_no_user_provided() {
        val sut = MemoryUserIdentityProvider()
        val result = sut.calculateIdentity(null, null)
        Assert.assertFalse(result.isEmpty())
    }

    @Test
    fun sessionIdentity_is_stable_within_instance() {
        val sut = MemoryUserIdentityProvider()
        val first = sut.calculateIdentity(null, null)
        val second = sut.calculateIdentity(null, null)
        Assert.assertEquals(first, second)
    }

    @Test
    fun sessionIdentity_differs_between_instances() {
        val sut1 = MemoryUserIdentityProvider()
        val sut2 = MemoryUserIdentityProvider()
        val id1 = sut1.calculateIdentity(null, null)
        val id2 = sut2.calculateIdentity(null, null)
        Assert.assertNotEquals(id1, id2)
    }
}
