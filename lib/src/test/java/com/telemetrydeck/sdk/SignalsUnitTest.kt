package com.telemetrydeck.sdk

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URL
import java.util.UUID
import kotlin.time.Clock

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
            mapOf("platform" to "Android")
        )

        assertEquals("type", signal.type)
        assertEquals("clientUser", signal.clientUser)
        assertEquals(JsonPrimitive("Android"), signal.payload["platform"])
    }

    @Test
    fun signal_serialize_date() {
        val receivedAt = Clock.System.now()

        val signal = Signal(UUID.randomUUID(), "type", "clientUser")
        signal.receivedAt = receivedAt

        val signalJson = Json.encodeToString(signal)
        val decodedSignal = Json.decodeFromString<Signal>(signalJson)

        assertEquals(receivedAt.toEpochMilliseconds(), decodedSignal.receivedAt.toEpochMilliseconds())
    }

    @Test
    fun signal_serialize_uuid() {
        val appID = UUID.randomUUID()

        val signal = Signal(appID, "type", "clientUser")

        val signalJson = Json.encodeToString(signal)
        val decodedSignal = Json.decodeFromString<Signal>(signalJson)

        // uuid comparison to the string representation from UUID to ensure equal casing
        assertEquals(appID, decodedSignal.appID)
    }

    @Test
    fun telemetryClient_correct_service_url() {
        val client = TelemetryClient(URL("https://nom.telemetrydeck.com"), false, null, null)

        val endpointUrl = client.getServiceUrl()

        assertEquals(
            "https://nom.telemetrydeck.com/v2/",
            endpointUrl.toString()
        )
    }

    @Test
    fun telemetryClient_correct_service_url_with_namespace() {
        val client = TelemetryClient(URL("https://nom.telemetrydeck.com"), false, "deltaquadrant", null)

        val endpointUrl = client.getServiceUrl()

        assertEquals(
            "https://nom.telemetrydeck.com/v2/namespace/deltaquadrant/",
            endpointUrl.toString()
        )
    }


    @Test
    fun signal_serialize_floatValue() {
        val float = 3.444444444444445

        val signal = Signal(UUID.randomUUID(), "type", "clientUser")
        signal.floatValue = float

        val signalJson = Json.encodeToString(signal)
        val decodedSignal = Json.decodeFromString<Signal>(signalJson)

        assertEquals(float, decodedSignal.floatValue)
    }
}