package com.telemetrydeck.sdk.providers

import com.telemetrydeck.sdk.TelemetryDeck
import com.telemetrydeck.sdk.TelemetryStorage
import com.telemetrydeck.sdk.signals.Acquisition
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FailingStorageLifecycleTest {

    private val failingStorage = object : TelemetryStorage {
        override fun read(key: String): String? = throw IOException("EACCES")
        override fun write(key: String, value: String) = throw IOException("ENOSPC")
        override fun delete(key: String) = throw IOException("EIO")
    }

    private fun buildManager(): TelemetryDeck = TelemetryDeck.Builder()
        .appID("32CB6574-6732-4238-879F-582FEBEB6536")
        .storage(failingStorage)
        .build(RuntimeEnvironment.getApplication())

    @Test
    fun sessionTracking_lifecycle_callbacks_survive_failing_storage() {
        val manager = buildManager()
        val sessionProvider = manager.sessionManager as SessionTrackingSignalProvider

        sessionProvider.onStart(mockk(relaxed = true))
        sessionProvider.onStop(mockk(relaxed = true))

        val signals = manager.cache?.empty() ?: emptyList()
        Assert.assertEquals(1, signals.count { it.type == Acquisition.NewInstallDetected.signalName })
        Assert.assertNotNull(manager.sessionID)
    }

    @Test
    fun durationTracking_lifecycle_callbacks_survive_failing_storage() {
        val manager = buildManager()
        val durationProvider = manager.providers.filterIsInstance<DurationSignalTrackerProvider>().single()

        durationProvider.onStart(mockk(relaxed = true))
        durationProvider.onStop(mockk(relaxed = true))
        manager.startDurationSignal("duration", emptyMap(), false)
        manager.stopAndSendDurationSignal("duration", emptyMap(), null, null)

        val signals = manager.cache?.empty() ?: emptyList()
        Assert.assertEquals(1, signals.count { it.type == "duration" })
    }
}
