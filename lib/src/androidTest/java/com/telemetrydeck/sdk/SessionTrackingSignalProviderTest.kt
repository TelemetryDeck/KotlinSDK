package com.telemetrydeck.sdk

import android.app.Application
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.telemetrydeck.sdk.params.Acquisition
import com.telemetrydeck.sdk.params.Retention
import com.telemetrydeck.sdk.providers.SessionTrackingSignalProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@RunWith(AndroidJUnit4::class)
class SessionTrackingSignalProviderTest {

    @UiThreadTest
    @Test
    fun single_session_is_recorded() {
        val todayString = "2025-02-22"
        val sut = createSut()


        sut.handleOnForeground(parseDateString("2025-02-22T11:00:00.000Z"))
        sut.handleOnBackground(parseDateString("2025-02-22T11:04:00.000Z"))


        val state = readState()
        assertNotNull(state)
        assertNotNull(state!!.lastEnteredBackground)
        assertEquals(1, state.distinctDays.size)
        assert(state.distinctDays.contains(todayString))
        assertEquals(1,state.sessions.size)
        assertEquals(1L, state.lifetimeSessionsCount)

        val session = state.sessions.first()
        // since we haven't started a new session, as per the session should still be considered ongoing
        assertNull(session.ended)
        assertEquals(0L, session.durationMillis)
        assertEquals(1, state.sessions.size)
    }

    @UiThreadTest
    @Test
    fun resuming_session_from_memory_within_5_minutes_resumes_session() {
        val todayString = "2025-02-22"
        val sut = createSut()


        sut.handleOnForeground(parseDateString("2025-02-22T11:00:00.000Z"))
        sut.handleOnBackground(parseDateString("2025-02-22T12:00:00.000Z"))
        sut.handleOnForeground(parseDateString("2025-02-22T12:04:00.000Z"))


        val state = readState()
        assertNotNull(state)
        assertNotNull(state!!.lastEnteredBackground)
        assertEquals(1, state.distinctDays.size)
        assert(state.distinctDays.contains(todayString))
        assertEquals(1, state.sessions.size)
        assertEquals(1L, state.lifetimeSessionsCount)

        val session = state.sessions.first()
        assertNull(session.ended)
        assertEquals(0L, session.durationMillis)
        assertEquals(1, state.sessions.size)
    }

    @UiThreadTest
    @Test
    fun resuming_session_from_memory_beyond_5_minutes_starts_session() {
        val todayString = "2025-02-22"
        val sut = createSut()


        sut.handleOnForeground(parseDateString("2025-02-22T11:00:00.000Z"))
        sut.handleOnBackground(parseDateString("2025-02-22T12:00:00.000Z"))
        sut.handleOnForeground(parseDateString("2025-02-22T12:05:01.000Z"))


        val state = readState()
        assertNotNull(state)
        assertNotNull(state!!.lastEnteredBackground)
        assertEquals(1, state.distinctDays.size)
        assert(state.distinctDays.contains(todayString))
        assertEquals(2, state.sessions.size)
        assertEquals(2L, state.lifetimeSessionsCount)

        val session = state.sessions.first()
        assertNotNull(session.ended)
        assertEquals("2025-02-22T12:05:01.000Z", formatTimeStamp(session.ended!!)) // session end time matches start time of next session
        assertEquals(3901000L, session.durationMillis)
        assertEquals(2, state.sessions.size)
    }

    @UiThreadTest
    @Test
    fun resuming_session_from_disk_within_5_minutes_resumes_session() {
        val todayString = "2025-02-22"
        val now = parseDateString("2025-02-22T11:04:00.000Z")
        val sut = createSut(stateFromJson("""
            {
              "sessions": [
                {
                  "firstStart": "2025-02-22T10:00:00.000Z",
                  "ended": null,
                  "durationMillis": 0
                }
              ],
              "distinctDays": ["2025-02-22"],
              "lastEnteredBackground": "2025-02-22T11:00:00.000Z",
              "lifetimeSessionsCount": 1
            }
        """.trimIndent()))


        sut.handleOnForeground(now)
        val state = readState()
        assert(state != null)
        assertEquals(1, state!!.distinctDays.size)
        assert(state.distinctDays.contains(todayString))
        assertEquals(1, state.sessions.size)
        assertEquals(1L, state.lifetimeSessionsCount)

        val session = state.sessions.first()
        assertNull(session.ended)
        assertEquals(0L, session.durationMillis)
        assertEquals(1, state.sessions.size)
    }

    @UiThreadTest
    @Test
    fun resuming_session_from_disk_within_5_minutes_starts_new_session() {
        val todayString = "2025-02-22"
        val now = parseDateString("2025-02-22T12:05:01.000Z")
        val sut = createSut(stateFromJson("""
            {
              "sessions": [
                {
                  "firstStart": "2025-02-22T10:00:00.000Z",
                  "ended": "2025-02-22T11:05:01.000Z",
                  "durationMillis": 3901000
                }
              ],
              "distinctDays": ["2025-02-22"],
              "lifetimeSessionsCount": 1,
              "lastEnteredBackground": "2025-02-22T11:00:00.000Z"
            }
        """.trimIndent()))


        sut.handleOnForeground(now)

        val state = readState()
        assertNotNull(state)
        assertNotNull(state!!.lastEnteredBackground)
        assertEquals(1, state.distinctDays.size)
        assert(state.distinctDays.contains(todayString))
        assertEquals(2, state.sessions.size)
        assertEquals(2L, state.lifetimeSessionsCount)

        val session = state.sessions.first()
        assertNotNull(session.ended)
        assertEquals("2025-02-22T12:05:01.000Z", formatTimeStamp(session.ended!!)) // session end time matches start time of next session
        assertEquals(3901000L, session.durationMillis)
        assertEquals(2, state.sessions.size)
    }


    @UiThreadTest
    @Test
    fun enrich_signals_with_resumed_unfinished_session() {
        val sut = createSut()


        sut.handleOnForeground(parseDateString("2025-02-22T11:00:00.000Z"))
        sut.handleOnBackground(parseDateString("2025-02-22T12:00:00.000Z"))
        sut.handleOnForeground(parseDateString("2025-02-22T12:04:00.000Z"))


        val attributes = sut.enrich("signal1", "clientUser", mapOf("param1" to "value1"))
        assertEquals("2025-02-22", attributes[Acquisition.FirstSessionDate.paramName])
        assertNull(attributes[Retention.AverageSessionSeconds.paramName])
        assertEquals("1", attributes[Retention.DistinctDaysUsed.paramName])
        assertEquals("1", attributes[Retention.TotalSessionsCount.paramName])
        assertNull(attributes[Retention.PreviousSessionSeconds.paramName])
    }

    @UiThreadTest
    @Test
    fun enrich_signals_with_second_session_in_progress() {
        val sut = createSut()


        sut.handleOnForeground(parseDateString("2025-02-22T11:00:00.000Z"))
        sut.handleOnBackground(parseDateString("2025-02-22T12:00:00.000Z"))
        sut.handleOnForeground(parseDateString("2025-02-22T12:05:01.000Z"))


        val attributes = sut.enrich("signal1", "clientUser", mapOf("param1" to "value1"))
        assertEquals("2025-02-22", attributes[Acquisition.FirstSessionDate.paramName])
        assertEquals("3901", attributes[Retention.AverageSessionSeconds.paramName])
        assertEquals("1", attributes[Retention.DistinctDaysUsed.paramName])
        assertEquals("2", attributes[Retention.TotalSessionsCount.paramName])
        assertEquals("3901.000", attributes[Retention.PreviousSessionSeconds.paramName])
    }

    @UiThreadTest
    @Test
    fun enrich_signals_with_second_session_in_progress_multiple_days() {
        val sut = createSut()


        sut.handleOnForeground(parseDateString("2025-02-22T11:00:00.000Z"))
        sut.handleOnBackground(parseDateString("2025-02-22T12:00:00.000Z"))
        sut.handleOnForeground(parseDateString("2025-02-23T01:00:00.000Z"))


        val attributes = sut.enrich("signal1", "clientUser", mapOf("param1" to "value1"))
        assertEquals("2025-02-22", attributes[Acquisition.FirstSessionDate.paramName])
        assertEquals("50400", attributes[Retention.AverageSessionSeconds.paramName])
        assertEquals("2", attributes[Retention.DistinctDaysUsed.paramName])
        assertEquals("2", attributes[Retention.TotalSessionsCount.paramName])
        assertEquals("50400.000", attributes[Retention.PreviousSessionSeconds.paramName])
    }


    @UiThreadTest
    @Test
    fun enrich_signals_from_multiple_sessions() {
        val sut = createSut()


        sut.handleOnForeground(parseDateString("2025-02-22T11:00:00.000Z"))

        sut.handleOnBackground(parseDateString("2025-02-22T12:00:00.000Z"))
        sut.handleOnForeground(parseDateString("2025-02-23T01:00:00.000Z")) // 50400

        sut.handleOnBackground(parseDateString("2025-02-23T01:00:10.000Z"))
        sut.handleOnForeground(parseDateString("2025-02-23T02:00:00.000Z")) // 3600


        val attributes = sut.enrich("signal1", "clientUser", mapOf("param1" to "value1"))
        assertEquals("2025-02-22", attributes[Acquisition.FirstSessionDate.paramName])
        assertEquals("27000", attributes[Retention.AverageSessionSeconds.paramName])
        assertEquals("2", attributes[Retention.DistinctDaysUsed.paramName])
        assertEquals("3", attributes[Retention.TotalSessionsCount.paramName])
        assertEquals("3600.000", attributes[Retention.PreviousSessionSeconds.paramName])
    }


    // Helpers
    private fun createSut(state: SessionTrackingSignalProvider.TrackingState? = null): SessionTrackingSignalProvider {
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val file = File(appContext.filesDir, "telemetrydeckstracking")
        if (file.exists()) {
            file.delete()
        }
        if (state != null) {
            prepareSutState(state)
        }
        val sut = SessionTrackingSignalProvider()
        sut.register(appContext, TelemetryDeck(configuration = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536"), providers = emptyList()))
        return sut
    }

    private fun prepareSutState(state: SessionTrackingSignalProvider.TrackingState) {
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val file = File(appContext.filesDir, "telemetrydeckstracking")
        val value = Json.encodeToString(state)
        file.writeText(value)
    }

    private fun stateFromJson(json: String): SessionTrackingSignalProvider.TrackingState {
        val state = Json.decodeFromString<SessionTrackingSignalProvider.TrackingState>(json)
        return state
    }

    private fun readState(): SessionTrackingSignalProvider.TrackingState? {
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val file = File(appContext.filesDir, "telemetrydeckstracking")
        if (file.exists()) {
            val json = file.readText()
            return stateFromJson(json)
        }
        return null
    }
    
    private fun formatTimeStamp(date: Date): String {
        return  SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(date)
    }

    private fun parseDateString(dateString: String): Date {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        return dateFormat.parse(dateString)!!
    }
}