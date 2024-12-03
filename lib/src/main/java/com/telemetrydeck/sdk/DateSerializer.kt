package com.telemetrydeck.sdk

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Serializer(forClass = Date::class)
internal object DateSerializer : KSerializer<Date> {
    private val df: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    init {
        val tz = TimeZone.getTimeZone("UTC")
        df.timeZone = tz
    }

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeString(df.format(value))
    }

    override fun deserialize(decoder: Decoder): Date {
        return df.parse(decoder.decodeString())!!
    }
}