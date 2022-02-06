package com.telemetrydeck.sdk

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.*

class PersistentSignalCacheTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var folder = TemporaryFolder()

    @Test
    fun persistentSignalCache_starts_with_empty_queue() {
        val cacheDir = folder.newFolder()
        val sut = PersistentSignalCache(cacheDir, null)

        Assert.assertEquals(0, sut.count())
    }

    @Test
    fun persistentSignalCache_creates_cache_file() {
        val cacheDir = folder.newFolder()
        val sut = PersistentSignalCache(cacheDir, null)

        val cacheFile = File(cacheDir, sut.cacheFileName)

        Assert.assertTrue(cacheFile.exists())

        val json = cacheFile.readText()
        val signals = Json.decodeFromString<List<Signal>>(json)
        Assert.assertEquals(0, signals.count())
    }

    @Test
    fun persistentSignalCache_new_signals_added_to_cache() {
        val cacheDir = folder.newFolder()
        val sut = PersistentSignalCache(cacheDir, null)
        sut.add(Signal(UUID.randomUUID(), "type", "user", SignalPayload()))
        val cacheFile = File(cacheDir, sut.cacheFileName)

        val json = cacheFile.readText()
        val signals = Json.decodeFromString<List<Signal>>(json)
        Assert.assertEquals(1, signals.count())
        Assert.assertEquals("type", signals[0].type)
        Assert.assertEquals("user", signals[0].clientUser)
    }

    @Test
    fun persistentSignalCache_empty_clears_cache() {
        val cacheDir = folder.newFolder()
        val sut = PersistentSignalCache(cacheDir, null)
        sut.add(Signal(UUID.randomUUID(), "type", "user", SignalPayload()))
        sut.empty()
        val cacheFile = File(cacheDir, sut.cacheFileName)

        val json = cacheFile.readText()
        val signals = Json.decodeFromString<List<Signal>>(json)
        Assert.assertEquals(0, signals.count())
    }

    @DelicateCoroutinesApi
    @Test
    fun persistentSignalCache_allows_adding_signals_concurrently() = runBlocking {
        val cacheDir = folder.newFolder()
        val sut = PersistentSignalCache(cacheDir, null)

        // the cache should accept signals from concurrent coroutines
        val scope = CoroutineScope(newFixedThreadPoolContext(4, "pool"))
        scope.launch {
            val coroutines = 1.rangeTo(100).map {
                // create 100 coroutines (light-weight threads).
                launch {
                    for (i in 1..20) { // and in each of them, add 20 signals
                        sut.add(Signal(UUID.randomUUID(), "type", "user", SignalPayload()))
                    }
                }
            }

            coroutines.forEach { coroutine ->
                coroutine.join() // wait for all coroutines to finish their jobs.
            }
        }.join()

        val cacheFile = File(cacheDir, sut.cacheFileName)

        val json = cacheFile.readText()
        val signals = Json.decodeFromString<List<Signal>>(json)
        Assert.assertEquals(2000, signals.count())
    }
}