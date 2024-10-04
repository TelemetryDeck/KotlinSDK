package com.telemetrydeck.sdk

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Signal(
    /**
     * When was this signal generated
     */
    @Serializable(with = DateSerializer::class)
    var receivedAt: Date = Date(),

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
     * Tags in the form "key:value" to attach to the signal
     */
    var payload: List<String>,

    /**
     * If "true", mark the signal as a testing signal and only show it in a dedicated test mode UI
     */
    var isTestMode: String = "false",

    /**
     * An optional floating-point value to include with the signal. Default is `nil`.
     */
    var floatValue: Double? = null
) {
    constructor(appID: UUID, signalType: String, clientUser: String, payload: SignalPayload) : this(appID=appID, type=signalType, clientUser = clientUser, payload = payload.asMultiValueDimension)
}
