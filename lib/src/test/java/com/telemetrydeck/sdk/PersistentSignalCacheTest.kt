package com.telemetrydeck.sdk

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class PersistentSignalCacheTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var folder = TemporaryFolder()

    private fun makeSignal(type: String = "type", user: String = "user") =
        Signal(UUID.randomUUID(), type, user, SignalPayload())

    @Test
    fun persistentSignalCache_starts_with_empty_queue() = runTest(UnconfinedTestDispatcher()) {
        val cacheDir = folder.newFolder()
        val sut = PersistentSignalCache(cacheDir, null, ioDispatcher = UnconfinedTestDispatcher())

        Assert.assertEquals(0, sut.count())
    }

    @Test
    fun persistentSignalCache_creates_cache_file() = runTest(UnconfinedTestDispatcher()) {
        val cacheDir = folder.newFolder()
        val sut = PersistentSignalCache(cacheDir, null, ioDispatcher = UnconfinedTestDispatcher())

        val cacheFile = File(cacheDir, sut.cacheFileName)

        Assert.assertTrue(cacheFile.exists())

        val json = cacheFile.readText()
        val signals = Json.decodeFromString<List<Signal>>(json)
        Assert.assertEquals(0, signals.count())
    }

    @Test
    fun persistentSignalCache_new_signals_added_to_cache() = runTest(UnconfinedTestDispatcher()) {
        val cacheDir = folder.newFolder()
        val sut = PersistentSignalCache(cacheDir, null, ioDispatcher = UnconfinedTestDispatcher())
        sut.add(makeSignal("type", "user"))
        val cacheFile = File(cacheDir, sut.cacheFileName)

        val json = cacheFile.readText()
        val signals = Json.decodeFromString<List<Signal>>(json)
        Assert.assertEquals(1, signals.count())
        Assert.assertEquals("type", signals[0].type)
        Assert.assertEquals("user", signals[0].clientUser)
    }

    @Test
    fun persistentSignalCache_empty_clears_cache() = runTest(UnconfinedTestDispatcher()) {
        val cacheDir = folder.newFolder()
        val sut = PersistentSignalCache(cacheDir, null, ioDispatcher = UnconfinedTestDispatcher())
        sut.add(makeSignal())
        sut.empty()
        val cacheFile = File(cacheDir, sut.cacheFileName)

        val json = cacheFile.readText()
        val signals = Json.decodeFromString<List<Signal>>(json)
        Assert.assertEquals(0, signals.count())
    }

    @Test
    fun cacheLimit_evictsOldestOnOverflow() = runTest(UnconfinedTestDispatcher()) {
        val cacheDir = folder.newFolder()
        val sut = PersistentSignalCache(cacheDir, null, cacheLimit = 3, ioDispatcher = UnconfinedTestDispatcher())
        for (i in 1..5) {
            sut.add(makeSignal("type$i"))
        }

        Assert.assertEquals(3, sut.count())
        val cacheFile = File(cacheDir, sut.cacheFileName)
        val json = cacheFile.readText()
        val signals = Json.decodeFromString<List<Signal>>(json)
        Assert.assertEquals(3, signals.count())
        Assert.assertEquals("type3", signals[0].type)
        Assert.assertEquals("type4", signals[1].type)
        Assert.assertEquals("type5", signals[2].type)
    }

    @Test
    fun restoreTrimsToLimit() = runTest(UnconfinedTestDispatcher()) {
        val cacheDir = folder.newFolder()
        val cacheFile = File(cacheDir, "telemetrydeck.json")
        val preexisting = (1..5).map { i -> makeSignal("type$i") }
        cacheFile.writeText(Json.encodeToString(preexisting))

        val sut = PersistentSignalCache(cacheDir, null, cacheLimit = 3, ioDispatcher = UnconfinedTestDispatcher())

        Assert.assertEquals(3, sut.count())
        val signals = sut.empty()
        Assert.assertEquals("type3", signals[0].type)
        Assert.assertEquals("type4", signals[1].type)
        Assert.assertEquals("type5", signals[2].type)
    }

    @Test
    fun defaultLimitIsTenThousand() = runTest(UnconfinedTestDispatcher()) {
        val cacheDir = folder.newFolder()
        val sut = PersistentSignalCache(cacheDir, null, ioDispatcher = UnconfinedTestDispatcher())
        for (i in 1..10_001) {
            sut.add(makeSignal("type$i"))
        }

        Assert.assertEquals(10_000, sut.count())
    }

    @DelicateCoroutinesApi
    @Test
    fun persistentSignalCache_allows_adding_signals_concurrently() = runBlocking {
        val cacheDir = folder.newFolder()
        val sut = PersistentSignalCache(cacheDir, null, ioDispatcher = UnconfinedTestDispatcher())

        val scope = CoroutineScope(newFixedThreadPoolContext(4, "pool"))
        scope.launch {
            val coroutines = 1.rangeTo(100).map {
                launch {
                    for (i in 1..20) {
                        sut.add(makeSignal())
                    }
                }
            }
            coroutines.forEach { it.join() }
        }.join()

        Assert.assertEquals(2000, sut.count())
    }

    @Test
    fun addAll_appendsAllSignals() = runTest(UnconfinedTestDispatcher()) {
        val cacheDir = folder.newFolder()
        val sut = PersistentSignalCache(cacheDir, null, ioDispatcher = UnconfinedTestDispatcher())
        sut.addAll((1..5).map { makeSignal("type$it") })

        Assert.assertEquals(5, sut.count())
        val cacheFile = File(cacheDir, sut.cacheFileName)
        val onDisk = Json.decodeFromString<List<Signal>>(cacheFile.readText())
        Assert.assertEquals(5, onDisk.count())
    }

    @Test
    fun addAll_respectsCacheLimit() = runTest(UnconfinedTestDispatcher()) {
        val cacheDir = folder.newFolder()
        val sut = PersistentSignalCache(cacheDir, null, cacheLimit = 3, ioDispatcher = UnconfinedTestDispatcher())
        sut.addAll((1..7).map { makeSignal("type$it") })

        Assert.assertEquals(3, sut.count())
        val survivors = sut.empty()
        Assert.assertEquals("type5", survivors[0].type)
        Assert.assertEquals("type6", survivors[1].type)
        Assert.assertEquals("type7", survivors[2].type)
    }

    @Test
    fun addAll_acrossExistingQueue_evictsOldest() = runTest(UnconfinedTestDispatcher()) {
        val cacheDir = folder.newFolder()
        val sut = PersistentSignalCache(cacheDir, null, cacheLimit = 3, ioDispatcher = UnconfinedTestDispatcher())
        sut.add(makeSignal("a"))
        sut.add(makeSignal("b"))
        sut.addAll(listOf(makeSignal("c"), makeSignal("d"), makeSignal("e")))

        Assert.assertEquals(3, sut.count())
        val survivors = sut.empty()
        Assert.assertEquals("c", survivors[0].type)
        Assert.assertEquals("d", survivors[1].type)
        Assert.assertEquals("e", survivors[2].type)
    }

    @Test
    fun addAll_emptyListIsNoop() = runTest(UnconfinedTestDispatcher()) {
        val cacheDir = folder.newFolder()
        val sut = PersistentSignalCache(cacheDir, null, ioDispatcher = UnconfinedTestDispatcher())
        sut.add(makeSignal("existing"))
        sut.addAll(emptyList())

        Assert.assertEquals(1, sut.count())
        val signals = sut.empty()
        Assert.assertEquals("existing", signals[0].type)
    }

    @Test
    fun add_doesNotBlockCallerOnDisk() = runTest {
        val cacheDir = folder.newFolder()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val sut = PersistentSignalCache(cacheDir, null, ioDispatcher = testDispatcher)

        sut.add(makeSignal("type"))

        val cacheFile = File(cacheDir, sut.cacheFileName)
        Assert.assertFalse("File must not exist before dispatcher runs", cacheFile.exists())

        advanceUntilIdle()

        Assert.assertTrue("File must exist after dispatcher runs", cacheFile.exists())
        val signals = Json.decodeFromString<List<Signal>>(cacheFile.readText())
        Assert.assertEquals(1, signals.count())
    }

    @Test
    fun rapid_adds_coalesce_to_single_write() = runTest {
        val cacheDir = folder.newFolder()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        var dispatchCount = 0

        val countingDispatcher = object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                dispatchCount++
                testDispatcher.dispatch(context, block)
            }
        }

        val sut = PersistentSignalCache(cacheDir, null, ioDispatcher = countingDispatcher)

        repeat(100) { i ->
            sut.add(makeSignal("type$i"))
        }

        advanceUntilIdle()

        val cacheFile = File(cacheDir, sut.cacheFileName)
        val signals = Json.decodeFromString<List<Signal>>(cacheFile.readText())
        Assert.assertEquals(100, signals.count())
        Assert.assertTrue(
            "Expected fewer than 10 dispatches for 100 adds, got $dispatchCount",
            dispatchCount < 10,
        )
    }
}
