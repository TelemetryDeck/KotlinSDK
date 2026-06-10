package com.telemetrydeck.sdk

import org.junit.Assert
import org.junit.Test
import java.util.UUID

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

    @Test
    fun cacheLimit_evictsOldestOnOverflow() {
        val sut = MemorySignalCache(cacheLimit = 3)
        for (i in 1..5) {
            sut.add(Signal(appID = UUID.randomUUID(), "type$i", "user", SignalPayload()))
        }

        Assert.assertEquals(3, sut.count())
        val signals = sut.empty()
        Assert.assertEquals("type3", signals[0].type)
        Assert.assertEquals("type4", signals[1].type)
        Assert.assertEquals("type5", signals[2].type)
    }

    @Test
    fun noEvictionBelowLimit() {
        val sut = MemorySignalCache(cacheLimit = 5)
        for (i in 1..3) {
            sut.add(Signal(appID = UUID.randomUUID(), "type$i", "user", SignalPayload()))
        }

        Assert.assertEquals(3, sut.count())
    }

    @Test
    fun defaultLimitIsUnbounded() {
        val sut = MemorySignalCache()
        for (i in 1..10_001) {
            sut.add(Signal(appID = UUID.randomUUID(), "type$i", "user", SignalPayload()))
        }

        Assert.assertEquals(10_001, sut.count())
    }

    @Test
    fun addAll_appendsAllSignals() {
        val sut = MemorySignalCache()
        sut.addAll((1..5).map { Signal(appID = UUID.randomUUID(), "type$it", "user", SignalPayload()) })

        Assert.assertEquals(5, sut.count())
    }

    @Test
    fun addAll_respectsCacheLimit() {
        val sut = MemorySignalCache(cacheLimit = 3)
        sut.addAll((1..7).map { Signal(appID = UUID.randomUUID(), "type$it", "user", SignalPayload()) })

        Assert.assertEquals(3, sut.count())
        val survivors = sut.empty()
        Assert.assertEquals("type5", survivors[0].type)
        Assert.assertEquals("type6", survivors[1].type)
        Assert.assertEquals("type7", survivors[2].type)
    }

    @Test
    fun addAll_acrossExistingQueue_evictsOldest() {
        val sut = MemorySignalCache(cacheLimit = 3)
        sut.add(Signal(appID = UUID.randomUUID(), "a", "user", SignalPayload()))
        sut.add(Signal(appID = UUID.randomUUID(), "b", "user", SignalPayload()))
        sut.addAll(listOf(
            Signal(appID = UUID.randomUUID(), "c", "user", SignalPayload()),
            Signal(appID = UUID.randomUUID(), "d", "user", SignalPayload()),
            Signal(appID = UUID.randomUUID(), "e", "user", SignalPayload()),
        ))

        Assert.assertEquals(3, sut.count())
        val survivors = sut.empty()
        Assert.assertEquals("c", survivors[0].type)
        Assert.assertEquals("d", survivors[1].type)
        Assert.assertEquals("e", survivors[2].type)
    }

    @Test
    fun addAll_emptyListIsNoop() {
        val sut = MemorySignalCache()
        sut.addAll(emptyList())

        Assert.assertEquals(0, sut.count())
    }
}