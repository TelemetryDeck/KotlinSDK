package com.telemetrydeck.sdk

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant


internal object DateSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        val epochMillis = value.toEpochMilliseconds()
        val epochSeconds = Math.floorDiv(epochMillis, 1000L)
        val millisOfSecond = Math.floorMod(epochMillis, 1000L)
        val withoutFraction = Instant.fromEpochSeconds(epochSeconds).toString().removeSuffix("Z")
        val fraction = millisOfSecond.toString().padStart(3, '0')
        encoder.encodeString("$withoutFraction.${fraction}Z")
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}