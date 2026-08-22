package com.telemetrydeck.sdk

import com.telemetrydeck.sdk.providers.DurationSignalTrackerProvider
import com.telemetrydeck.sdk.providers.SessionTrackingSignalProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Instant

class DateSerializerTest {

    @Test
    fun decodes_whole_second_fixture() {
        val instant = Json.decodeFromString(DateSerializer, "\"2025-01-15T09:30:00.000Z\"")
        assertEquals(Instant.parse("2025-01-15T09:30:00.000Z").toEpochMilliseconds(), instant.toEpochMilliseconds())
    }

    @Test
    fun decodes_fractional_fixture() {
        val instant = Json.decodeFromString(DateSerializer, "\"2025-01-15T09:30:00.123Z\"")
        assertEquals(Instant.parse("2025-01-15T09:30:00.123Z").toEpochMilliseconds(), instant.toEpochMilliseconds())
    }

    @Test
    fun roundtrip_keeps_the_zero_fraction_on_whole_seconds_and_the_exact_fraction_otherwise() {
        val sources = listOf(
            "2025-01-15T09:30:00.000Z",
            "2025-01-15T09:30:00.123Z",
            "1999-12-31T23:59:59.999Z",
            "1970-01-01T00:00:00.000Z",
        )
        for (source in sources) {
            val instant = Instant.parse(source)
            val json = Json.encodeToString(DateSerializer, instant)
            assertEquals("\"$source\"", json)

            val decoded = Json.decodeFromString(DateSerializer, json)
            assertEquals(instant.toEpochMilliseconds(), decoded.toEpochMilliseconds())
        }
    }

    @Test
    fun signal_fixture_whole_second_decodes_and_reencodes_byte_identically() {
        val json = "{\"receivedAt\":\"2025-01-15T09:30:00.000Z\",\"appID\":\"32cb6574-6732-4238-879f-582febeb6536\",\"clientUser\":\"clientUser\",\"sessionID\":\"11111111-1111-1111-1111-111111111111\",\"type\":\"type\",\"payload\":[\"platform:Android\"]}"

        val signal = Json.decodeFromString<Signal>(json)

        assertEquals(Instant.parse("2025-01-15T09:30:00.000Z").toEpochMilliseconds(), signal.receivedAt.toEpochMilliseconds())
        assertEquals(UUID.fromString("32cb6574-6732-4238-879f-582febeb6536"), signal.appID)
        assertEquals("clientUser", signal.clientUser)
        assertEquals("11111111-1111-1111-1111-111111111111", signal.sessionID)
        assertEquals("type", signal.type)
        assertEquals(listOf("platform:Android"), signal.payload)

        assertEquals(json, Json.encodeToString(signal))
    }

    @Test
    fun signal_fixture_fractional_decodes_and_reencodes_byte_identically() {
        val json = "{\"receivedAt\":\"2025-01-15T09:30:00.123Z\",\"appID\":\"32cb6574-6732-4238-879f-582febeb6536\",\"clientUser\":\"clientUser\",\"sessionID\":\"11111111-1111-1111-1111-111111111111\",\"type\":\"type\",\"payload\":[\"platform:Android\"]}"

        val signal = Json.decodeFromString<Signal>(json)

        assertEquals(Instant.parse("2025-01-15T09:30:00.123Z").toEpochMilliseconds(), signal.receivedAt.toEpochMilliseconds())
        assertEquals(json, Json.encodeToString(signal))
    }

    @Test
    fun sessionTracking_state_fixture_decodes_and_reencodes_byte_identically() {
        val json = "{\"sessions\":[{\"id\":\"22222222-2222-2222-2222-222222222222\",\"firstStart\":\"2025-01-10T08:00:00.000Z\",\"ended\":\"2025-01-10T09:15:30.500Z\",\"durationMillis\":4530500}],\"distinctDays\":[\"2025-01-10\",\"2025-01-11\",\"2025-01-15\"],\"lifetimeSessionsCount\":3,\"lastEnteredBackground\":\"2025-01-15T09:29:00.000Z\"}"

        val state = Json.decodeFromString<SessionTrackingSignalProvider.TrackingState>(json)

        assertEquals(1, state.sessions.size)
        val session = state.sessions.first()
        assertEquals(UUID.fromString("22222222-2222-2222-2222-222222222222"), session.id)
        assertEquals(Instant.parse("2025-01-10T08:00:00.000Z").toEpochMilliseconds(), session.firstStart.toEpochMilliseconds())
        assertEquals(Instant.parse("2025-01-10T09:15:30.500Z").toEpochMilliseconds(), session.ended?.toEpochMilliseconds())
        assertEquals(4530500L, session.durationMillis)
        assertEquals(listOf("2025-01-10", "2025-01-11", "2025-01-15"), state.distinctDays)
        assertEquals(3L, state.lifetimeSessionsCount)
        assertEquals(Instant.parse("2025-01-15T09:29:00.000Z").toEpochMilliseconds(), state.lastEnteredBackground?.toEpochMilliseconds())

        assertEquals(json, Json.encodeToString(state))
    }

    @Test
    fun durationTracker_state_fixture_decodes_and_reencodes_byte_identically() {
        val json = "{\"signals\":{\"mySignal\":{\"startTime\":\"2025-01-15T09:00:00.000Z\",\"parameters\":{\"key\":\"value\"},\"includeBackgroundTime\":true}},\"lastEnteredBackground\":\"2025-01-15T09:29:45.250Z\"}"

        val state = Json.decodeFromString<DurationSignalTrackerProvider.TrackerState>(json)

        val cachedData = state.signals["mySignal"]
        assertEquals(Instant.parse("2025-01-15T09:00:00.000Z").toEpochMilliseconds(), cachedData?.startTime?.toEpochMilliseconds())
        assertEquals(mapOf("key" to "value"), cachedData?.parameters)
        assertEquals(true, cachedData?.includeBackgroundTime)
        assertEquals(Instant.parse("2025-01-15T09:29:45.250Z").toEpochMilliseconds(), state.lastEnteredBackground?.toEpochMilliseconds())

        assertEquals(json, Json.encodeToString(state))
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun concurrent_signal_encode_decode_is_safe() = runBlocking {
        val threadCount = 8
        val iterations = 200
        val errors = ConcurrentLinkedQueue<Throwable>()

        newFixedThreadPoolContext(threadCount, "date-serializer-stress").use { dispatcher ->
            CoroutineScope(dispatcher).launch {
                val jobs = (0 until threadCount).map { threadIndex ->
                    launch {
                        repeat(iterations) { i ->
                            try {
                                val epochMillis = 1_700_000_000_000L + threadIndex * 3_600_001L + i
                                val instant = Instant.fromEpochMilliseconds(epochMillis)
                                val signal = Signal(UUID.randomUUID(), "type", "user", SignalPayload())
                                signal.receivedAt = instant

                                val json = Json.encodeToString(signal)
                                val decoded = Json.decodeFromString<Signal>(json)

                                if (decoded.receivedAt.toEpochMilliseconds() != instant.toEpochMilliseconds()) {
                                    throw AssertionError("round trip mismatch: expected $instant but got ${decoded.receivedAt}")
                                }
                            } catch (t: Throwable) {
                                errors.add(t)
                            }
                        }
                    }
                }
                jobs.forEach { it.join() }
            }.join()
        }

        assertTrue("Concurrent encode/decode failed with ${errors.size} errors, first: ${errors.firstOrNull()}", errors.isEmpty())
    }

    @Test
    fun roundtrip_survives_a_non_ascii_digit_locale() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.Builder().setLanguage("ar").setRegion("SA").build())

            val wholeSecond = Instant.parse("2025-01-15T09:30:00.000Z")
            val fractional = Instant.parse("2025-01-15T09:30:00.123Z")

            assertEquals("\"2025-01-15T09:30:00.000Z\"", Json.encodeToString(DateSerializer, wholeSecond))
            assertEquals("\"2025-01-15T09:30:00.123Z\"", Json.encodeToString(DateSerializer, fractional))

            val decodedWhole = Json.decodeFromString(DateSerializer, "\"2025-01-15T09:30:00.000Z\"")
            val decodedFractional = Json.decodeFromString(DateSerializer, "\"2025-01-15T09:30:00.123Z\"")
            assertEquals(wholeSecond.toEpochMilliseconds(), decodedWhole.toEpochMilliseconds())
            assertEquals(fractional.toEpochMilliseconds(), decodedFractional.toEpochMilliseconds())
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
