package com.telemetrydeck.sdk

import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PersistentStorageTest {
    @get:Rule
    var folder = TemporaryFolder()

    @Test
    fun read_returnsNull_forMissingKey() {
        val sut = PersistentStorage(folder.newFolder(), null)

        Assert.assertNull(sut.read("missing"))
    }

    @Test
    fun read_returnsValue_afterWrite() {
        val sut = PersistentStorage(folder.newFolder(), null)
        sut.write("key", "value")

        Assert.assertEquals("value", sut.read("key"))
    }

    @Test
    fun write_overwritesExistingValue() {
        val sut = PersistentStorage(folder.newFolder(), null)
        sut.write("key", "first")
        sut.write("key", "second")

        Assert.assertEquals("second", sut.read("key"))
    }

    @Test
    fun write_persistsToDisk() {
        val filesDir = folder.newFolder()
        val sut = PersistentStorage(filesDir, null)
        sut.write("key", "value")

        val file = File(filesDir, "key")
        Assert.assertTrue(file.exists())
        Assert.assertEquals("value", file.readText())
    }

    @Test
    fun delete_removesValue() {
        val sut = PersistentStorage(folder.newFolder(), null)
        sut.write("key", "value")
        sut.delete("key")

        Assert.assertNull(sut.read("key"))
    }

    @Test
    fun delete_missingKey_isNoop() {
        val sut = PersistentStorage(folder.newFolder(), null)

        sut.delete("missing")

        Assert.assertNull(sut.read("missing"))
    }

    @Test
    fun roundTrip_survivesNewInstance() {
        val filesDir = folder.newFolder()
        PersistentStorage(filesDir, null).write("key", "value")

        val sut = PersistentStorage(filesDir, null)
        Assert.assertEquals("value", sut.read("key"))
    }
}
