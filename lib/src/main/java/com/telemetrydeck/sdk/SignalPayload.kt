package com.telemetrydeck.sdk

import android.icu.util.VersionInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToStringMap
import java.util.*

@Serializable
data class SignalPayload(
    var additionalPayload: Map<String, String> = emptyMap()
) {

    val asMap: Map<String, Any> by lazy {
        Properties.encodeToStringMap(this).filterKeys { !it.startsWith("additionalPayload") }
    }

    val asMultiValueDimension: List<String> by lazy {
        this.asMap.map { "${cleanKey(it.key)}:${it.value}" } + this.additionalPayload.map {
            "${
                cleanKey(
                    it.key
                )
            }:${it.value}"
        }
    }

    private fun cleanKey(key: String): String = key.replace(":", "_")
}
