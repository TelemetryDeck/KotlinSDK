package com.telemetrydeck.sdk

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
data class Signal(
    /**
     * When was this signal generated
     */
    @Serializable(with = DateSerializer::class)
    var receivedAt: Instant = Clock.System.now(),

    /**
     * The App ID of this signal
     */
    @Serializable(with = UUIDSerializer::class)
    var appID: UUID,

    /**
     *  A user identifier. This should be hashed on the client, and will be hashed + salted again on the server to break any connection to personally identifiable data.
     *
     */
    var clientUser: String,

    /**
     * A randomly generated session identifier. Should be the same over the course of the session
     */
    var sessionID: String? = null,


    /**
     * A type name for this signal that describes the event that triggered the signal
     */
    var type: String,


    /**
     * The additional parameters attached to the signal, as a JSON object of key-value pairs
     */
    var payload: JsonObject,

    /**
     * If "true", mark the signal as a testing signal and only show it in a dedicated test mode UI
     */
    var isTestMode: String = "false",

    /**
     * An optional floating-point value to include with the signal. Default is `nil`.
     */
    var floatValue: Double? = null
) {
    constructor(appID: UUID, signalType: String, clientUser: String, additionalPayload: Map<String, String> = emptyMap()) : this(
        appID = appID,
        type = signalType,
        clientUser = clientUser,
        payload = JsonObject(additionalPayload.toJsonPrimitives())
    )
}

/**
 * Converts a map of string parameters into a map of [JsonPrimitive] values, suitable for building a [JsonObject] payload.
 */
internal fun Map<String, String>.toJsonPrimitives(): Map<String, JsonElement> = mapValues { JsonPrimitive(it.value) }
