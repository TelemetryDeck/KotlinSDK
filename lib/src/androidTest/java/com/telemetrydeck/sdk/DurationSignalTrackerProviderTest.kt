package com.telemetrydeck.sdk

import android.app.Application
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.telemetrydeck.sdk.providers.DurationSignalTrackerProvider
import com.telemetrydeck.sdk.signals.Signal
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Date


@RunWith(AndroidJUnit4::class)
class DurationSignalTrackerProviderTest {

    // One or more digits, a dot, then exactly three digits
    val durationPrecisionFormat = Regex("^\\d+\\.\\d{3}$")

    private fun createSut(): DurationSignalTrackerProvider {
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val sut = DurationSignalTrackerProvider()
        sut.register(appContext, TelemetryDeck(configuration = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536"), providers = emptyList()))
        return sut
    }

    private fun prepareState(state: DurationSignalTrackerProvider.TrackerState) {
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val file = File(appContext.filesDir, "telemetrydeckduration")
        val value = Json.encodeToString(state)
        file.writeText(value)
    }

    @UiThreadTest
    @Test
    fun tracks_a_signal_duration() {
        val sut = createSut()
        sut.startTracking("signal1", emptyMap(), false)
        // sleep for 5 seconds
        Thread.sleep(5000)
        val params = sut.stopTracking("signal1", emptyMap())


        val value = params?.get(Signal.DurationInSeconds.signalName)

        // ensure precision of 3
        assert(value != null)
        assert(value!!.matches(durationPrecisionFormat))

        // assert a duration is present and bigger or euqal to 5
        val duration = value.toDouble()
        assert(duration >= 5.0)
    }

    @UiThreadTest
    @Test
    fun tracks_a_signal_duration_with_starting_and_endingparams() {
        val sut = createSut()
        sut.startTracking("signal1", mapOf("param1" to "value1"), false)
        // sleep for 5 seconds
        Thread.sleep(5000)
        val params = sut.stopTracking("signal1", mapOf("param2" to "value3"))

        assertEquals("value1", params?.get("param1"))
        assertEquals("value3", params?.get("param2"))

        val value = params?.get(Signal.DurationInSeconds.signalName)

        // ensure precision of 3
        assert(value != null)
        assert(value!!.matches(durationPrecisionFormat))

        // assert a duration is present and bigger or euqal to 5
        val duration = value.toDouble()
        assert(duration >= 5.0)
    }

    @UiThreadTest
    @Test
    fun restores_state_from_disk() {
        val now = Date()
        val twoHoursAgo = Date(now.time - 7200000)
        val hourAgo = Date(now.time - 3600000)
        val state = DurationSignalTrackerProvider.TrackerState(
            signals = mapOf("signal1" to DurationSignalTrackerProvider.CachedData(startTime = twoHoursAgo, parameters = mapOf("param1" to "value1"), false)),
            lastEnteredBackground = hourAgo
        )
        prepareState(state)

        val sut = createSut()
        val params = sut.stopTracking("signal1", mapOf("param2" to "value3"))

        assertEquals("value1", params?.get("param1"))
        assertEquals("value3", params?.get("param2"))

        val value = params?.get(Signal.DurationInSeconds.signalName)

        // ensure precision of 3
        assert(value != null)
        assert(value!!.matches(durationPrecisionFormat))

        val duration = value.toDouble()
        assert(duration > 7200)
    }

    @UiThreadTest
    @Test
    fun substracts_background_time() {
        val now = Date()
        val twoHoursAgo = Date(now.time - 7200000)
        val hourAgo = Date(now.time - 3700000)
        val state = DurationSignalTrackerProvider.TrackerState(
            signals = mapOf("signal1" to DurationSignalTrackerProvider.CachedData(startTime = twoHoursAgo, parameters = mapOf("param1" to "value1"), false)),
            lastEnteredBackground = hourAgo
        )
        prepareState(state)
        val sut = createSut()

        sut.handleOnForeground()
        val params = sut.stopTracking("signal1", mapOf("param2" to "value3"))

        assertEquals("value1", params?.get("param1"))
        assertEquals("value3", params?.get("param2"))

        val value = params?.get(Signal.DurationInSeconds.signalName)

        // ensure precision of 3
        assert(value != null)
        assert(value!!.matches(durationPrecisionFormat))

        val duration = value.toDouble()
        assert(duration < 3600)
    }
}