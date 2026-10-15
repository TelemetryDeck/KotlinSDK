package com.telemetrydeck.sdk

import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.Assert
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class PersistentSignalCacheIoFailureTest {
    @get:Rule
    var folder = TemporaryFolder()

    @Test
    fun ioErrorWhileSavingDoesNotReachUncaughtExceptionHandler() {
        val uncaught = AtomicReference<Throwable?>()
        val reported = CountDownLatch(1)
        val ioDispatcher = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable).apply {
                setUncaughtExceptionHandler { _, e ->
                    uncaught.set(e)
                    reported.countDown()
                }
            }
        }.asCoroutineDispatcher()
        val logger = object : DebugLogger {
            override fun error(message: String) = reported.countDown()
            override fun debug(message: String) {}
            override fun configure(enabled: Boolean) {}
        }

        val cacheDir = folder.newFolder()
        try {
            val sut = PersistentSignalCache(cacheDir, logger, ioDispatcher = ioDispatcher)
            val cacheFile = File(cacheDir, sut.cacheFileName)
            waitUntil { cacheFile.length() > 0 }

            cacheFile.delete()
            cacheDir.setWritable(false)
            Assume.assumeFalse("permissions are not enforced (running as root?)", cacheDir.canWrite())

            sut.add(Signal(UUID.randomUUID(), "type", "user"))

            Assert.assertTrue(reported.await(2, TimeUnit.SECONDS))
            cacheDir.setWritable(true)
            uncaught.get()?.let {
                throw AssertionError("saveSignals() let an exception escape to the thread's uncaught exception handler", it)
            }

            sut.add(Signal(UUID.randomUUID(), "type", "user"))
            waitUntil { cacheFile.exists() && cacheFile.readText().contains("type") }
            Assert.assertEquals(2, sut.count())
        } finally {
            cacheDir.setWritable(true)
            ioDispatcher.close()
        }
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 2_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "timed out" }
            Thread.sleep(10)
        }
    }
}
