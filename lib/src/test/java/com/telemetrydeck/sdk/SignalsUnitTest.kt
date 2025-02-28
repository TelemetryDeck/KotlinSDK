package com.telemetrydeck.sdk

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URL
import java.util.Date
import java.util.UUID

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class SignalsUnitTest {
    @Test
    fun signal_secondary_constructor_sets_properties() {
        val signal = Signal(
            UUID.randomUUID(),
            "type",
            "clientUser",
            SignalPayload(additionalPayload = mapOf("platform" to "Android"))
        )

        assertEquals("type", signal.type)
        assertEquals("clientUser", signal.clientUser)
        assertEquals("platform:Android", signal.payload[0])
    }

    @Test
    fun signal_serialize_date() {
        val receivedDate = Date()

        val signal = Signal(UUID.randomUUID(), "type", "clientUser", SignalPayload())
        signal.receivedAt = receivedDate

        val signalJson = Json.encodeToString(signal)
        val decodedSignal = Json.decodeFromString<Signal>(signalJson)

        assertDatesEqualIgnoringMilliseconds(receivedDate, decodedSignal.receivedAt)
    }

    fun truncateMilliseconds(date: Date): Date {
        return Date(date.time / 1000 * 1000) // Remove milliseconds by truncating to the nearest second
    }

    fun assertDatesEqualIgnoringMilliseconds(expected: Date, actual: Date) {
        val truncatedExpected = truncateMilliseconds(expected)
        val truncatedActual = truncateMilliseconds(actual)
        assertEquals(truncatedExpected, truncatedActual)
    }

    @Test
    fun signal_serialize_uuid() {
        val appID = UUID.randomUUID()

        val signal = Signal(appID, "type", "clientUser", SignalPayload())

        val signalJson = Json.encodeToString(signal)
        val decodedSignal = Json.decodeFromString<Signal>(signalJson)

        // uuid comparison to the string representation from UUID to ensure equal casing
        assertEquals(appID, decodedSignal.appID)
    }

    @Test
    fun signal_payload_asMultiValueDimension() {
        val payload = SignalPayload(additionalPayload = mapOf("key" to "value"))
        val value = payload.asMultiValueDimension

        // additional properties are included in the multi dimension value
        assertEquals(1, value.size)
        assertEquals("key:value", value[0])
    }

    @Test
    fun signal_payload_asMultiValueDimension_with_additional_properties_with_separator() {
        val payload = SignalPayload(additionalPayload = mapOf("key:with_separator" to "value"))

        val value = payload.asMultiValueDimension

        // keys with the field separator ':' are encoded with an underscore instead '_'
        assertEquals(1, value.size)
        assertEquals("key_with_separator:value", value[0])
    }

    @Test
    fun telemetryClient_correct_service_url() {
        val client = TelemetryClient(URL("https://nom.telemetrydeck.com"), false, null, null)

        val endpointUrl = client.getServiceUrl()

        // date equality comparison with precision up to milliseconds
        assertEquals(
            "https://nom.telemetrydeck.com/v2/",
            endpointUrl.toString()
        )
    }

    @Test
    fun telemetryClient_correct_service_url_with_namespace() {
        val client = TelemetryClient(URL("https://nom.telemetrydeck.com"), false, "deltaquadrant", null)

        val endpointUrl = client.getServiceUrl()

        // date equality comparison with precision up to milliseconds
        assertEquals(
            "https://nom.telemetrydeck.com/v2/namespace/deltaquadrant/",
            endpointUrl.toString()
        )
    }


    @Test
    fun signal_serialize_floatValue() {
        val float = 3.444444444444445

        val signal = Signal(UUID.randomUUID(), "type", "clientUser", SignalPayload())
        signal.floatValue = float

        val signalJson = Json.encodeToString(signal)
        val decodedSignal = Json.decodeFromString<Signal>(signalJson)

        // date equality comparison with precision up to milliseconds
        assertEquals(float, decodedSignal.floatValue)
    }
}