package com.telemetrydeck.sdk

import org.junit.Assert
import org.junit.Test
import java.util.*

class MemorySignalCacheTest {
    @Test
    fun memorySignalCache_starts_empty() {
        val sut = MemorySignalCache()

        Assert.assertEquals(0, sut.count())
    }

    @Test
    fun memorySignalCache_add_signal() {
        val signal = Signal(appID = UUID.randomUUID(), "type", "user", SignalPayload())

        val sut = MemorySignalCache()
        sut.add(signal)

        Assert.assertEquals(1, sut.count())
    }

    @Test
    fun memorySignalCache_empty_signals() {
        val signal1 = Signal(appID = UUID.randomUUID(), "type", "user", SignalPayload())
        val signal2 = Signal(appID = UUID.randomUUID(), "type", "user", SignalPayload())

        val sut = MemorySignalCache()
        sut.add(signal1)
        sut.add(signal2)

        val signals = sut.empty()

        Assert.assertEquals(0, sut.count())
        Assert.assertEquals(2, signals.count())
    }

    @Test
    fun memorySignalCache_read_signal_data() {
        val signal1 = Signal(appID = UUID.randomUUID(), "type1", "user1", SignalPayload())
        val signal2 = Signal(appID = UUID.randomUUID(), "type2", "user2", SignalPayload())

        val sut = MemorySignalCache()
        sut.add(signal1)
        sut.add(signal2)

        val signals = sut.empty()

        Assert.assertEquals(signal1.appID, signals[0].appID)
        Assert.assertEquals(signal1.type, signals[0].type)
        Assert.assertEquals(signal1.clientUser, signals[0].clientUser)
        Assert.assertEquals(signal2.appID, signals[1].appID)
        Assert.assertEquals(signal2.type, signals[1].type)
        Assert.assertEquals(signal2.clientUser, signals[1].clientUser)
    }
}