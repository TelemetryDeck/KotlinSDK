package com.telemetrydeck.sdk.providers

import com.telemetrydeck.sdk.TelemetryDeck
import com.telemetrydeck.sdk.params.Retention
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SessionTrackingDayHelperTest {

    private fun stateFile(): File {
        val context = RuntimeEnvironment.getApplication()
        return File(context.filesDir, "telemetrydeckstracking")
    }

    private fun createSut(): SessionTrackingSignalProvider {
        val file = stateFile()
        if (file.exists()) {
            file.delete()
        }
        val manager = TelemetryDeck.Builder()
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .build(RuntimeEnvironment.getApplication())
        return manager.sessionManager as SessionTrackingSignalProvider
    }

    private fun prepareState(state: SessionTrackingSignalProvider.TrackingState) {
        stateFile().writeText(Json.encodeToString(state))
    }

    private fun readDistinctDays(): List<String> {
        val state = Json.decodeFromString<SessionTrackingSignalProvider.TrackingState>(stateFile().readText())
        return state.distinctDays
    }

    @Test
    fun dayString_matches_a_reference_simpleDateFormat_across_instants_and_timezones() {
        val originalTimeZone = TimeZone.getDefault()
        try {
            val instants = listOf(
                Instant.parse("2025-01-15T09:30:00.000Z"),
                Instant.parse("2025-06-30T23:59:59.000Z"),
                Instant.parse("2025-12-31T00:00:01.000Z"),
            )
            val zoneIDs = listOf("UTC", "Europe/Brussels", "Pacific/Kiritimati")

            for (zoneID in zoneIDs) {
                TimeZone.setDefault(TimeZone.getTimeZone(zoneID))
                for (instant in instants) {
                    val referenceFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val expected = referenceFormat.format(Date(instant.toEpochMilliseconds()))

                    val sut = createSut()
                    sut.handleOnForeground(instant)

                    val distinctDays = readDistinctDays()
                    assertTrue(
                        "zone=$zoneID instant=$instant expected day $expected in $distinctDays",
                        distinctDays.contains(expected)
                    )
                }
            }
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }

    @Test
    fun corrupt_distinctDays_entry_is_dropped_without_losing_the_valid_entries() {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

            val sut = createSut()
            prepareState(
                SessionTrackingSignalProvider.TrackingState(
                    sessions = emptyList(),
                    distinctDays = listOf("2025-01-10", "not-a-valid-day", "2025-01-11"),
                    lifetimeSessionsCount = 2,
                )
            )
            // restores the fixture above and adds today (2025-01-15) as a fourth, valid entry
            sut.handleOnForeground(Instant.parse("2025-01-15T00:00:00.000Z"))

            val attributes = sut.enrichInternal(
                "signal1",
                "clientUser",
                emptyMap(),
                Instant.parse("2025-01-15T00:01:00.000Z")
            )

            // all four raw entries are counted, including the corrupt one...
            assertEquals("4", attributes[Retention.DistinctDaysUsed.paramName])
            // ...but only the three that parse successfully count toward the last-30-days window
            assertEquals("3", attributes[Retention.DistinctDaysUsedLastMonth.paramName])
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }
}
