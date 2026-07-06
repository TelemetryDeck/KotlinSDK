package com.telemetrydeck.sdk

import org.junit.Assert
import org.junit.Test

class InMemoryStorageTest {
    @Test
    fun read_returnsNull_forMissingKey() {
        val sut = InMemoryStorage()

        Assert.assertNull(sut.read("missing"))
    }

    @Test
    fun read_returnsValue_afterWrite() {
        val sut = InMemoryStorage()
        sut.write("key", "value")

        Assert.assertEquals("value", sut.read("key"))
    }

    @Test
    fun write_overwritesExistingValue() {
        val sut = InMemoryStorage()
        sut.write("key", "first")
        sut.write("key", "second")

        Assert.assertEquals("second", sut.read("key"))
    }

    @Test
    fun delete_removesValue() {
        val sut = InMemoryStorage()
        sut.write("key", "value")
        sut.delete("key")

        Assert.assertNull(sut.read("key"))
    }

    @Test
    fun delete_missingKey_isNoop() {
        val sut = InMemoryStorage()

        sut.delete("missing")

        Assert.assertNull(sut.read("missing"))
    }
}
