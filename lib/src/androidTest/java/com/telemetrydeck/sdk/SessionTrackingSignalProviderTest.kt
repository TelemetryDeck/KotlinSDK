package com.telemetrydeck.sdk

import android.app.Application
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
    private fun createSut(): SessionTrackingSignalProvider {
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val sut = SessionTrackingSignalProvider()
        sut.register(appContext, TelemetryDeck(configuration = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536"), providers = emptyList()))
        return sut
    }

    private fun prepareState(state: SessionTrackingSignalProvider.TrackingState) {
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val file = File(appContext.filesDir, "telemetrydeckstracking")
        val value = Json.encodeToString(state)
        file.writeText(value)
    }

    private fun readState(): SessionTrackingSignalProvider.TrackingState? {
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val file = File(appContext.filesDir, "telemetrydeckstracking")
        if (file.exists()) {
            val json = file.readText()
            val state = Json.decodeFromString<SessionTrackingSignalProvider.TrackingState>(json)
            return state
        }
        return null
    }

    private fun formatDate(date: Date): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
    }

    // One or more digits, a dot, then exactly three digits
    val durationPrecisionFormat = Regex("^\\d+\\.\\d{3}$")

    @UiThreadTest
    @Test
    fun track_single_session_remains_started() {
        val todayString = formatDate(Date())
        val sut = createSut()


        sut.handleOnForeground()
        Thread.sleep(5000)
        sut.handleOnBackground()


        val state = readState()
        assert(state != null)
        assert(state!!.distinctDays.size == 1)
        assert(state.distinctDays.contains(todayString))
        assert(state.sessions.size == 1)
        assertEquals(1L, state.lifetimeSessionsCount)

        val session = state.sessions.first()
        // since we haven't started a new session, as per the session should still be considered ongoing
        assertNull(session.ended)
        assertEquals(0L, session.durationMillis)
        assertEquals(1, state.sessions.size)

    }

    @UiThreadTest
    @Test
    fun track_two_sessions_first_session_ended() {
        val todayString = formatDate(Date())
        val sut = createSut()


        sut.handleOnForeground()
        Thread.sleep(5000)
        sut.handleOnBackground()
        Thread.sleep(6000)
        sut.handleOnForeground()


        val state = readState()
        assert(state != null)
        assert(state!!.distinctDays.size == 1)
        assert(state.distinctDays.contains(todayString))
        assertEquals(2, state.sessions.size)
        assertEquals(2L, state.lifetimeSessionsCount)

        val session1 = state.sessions[0]
        assertNotNull(session1.ended) // ended session
        assert(session1.durationMillis > 11000L)

        val session2 = state.sessions[1]
        assertNull(session2.ended) // not ended session
        assert(session2.durationMillis == 0L)
    }
}