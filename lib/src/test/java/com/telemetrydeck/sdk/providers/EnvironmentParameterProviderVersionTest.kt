package com.telemetrydeck.sdk.providers

import com.telemetrydeck.sdk.TelemetryDeck
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EnvironmentParameterProviderVersionTest {

    @Test
    fun nonNumericSystemVersionFallsBackToPlainParsing() {
        ShadowBuild.setVersionRelease("Baklava")
        val manager = TelemetryDeck.Builder()
            .appID("32CB6574-6732-4238-879F-582FEBEB6536")
            .build(null)

        manager.signal("type")

        val payload = manager.cache?.empty()?.first()?.payload
        Assert.assertEquals("Baklava", payload?.get("TelemetryDeck.Device.systemMajorVersion")?.jsonPrimitive?.content)
        Assert.assertEquals("Baklava.0", payload?.get("TelemetryDeck.Device.systemMajorMinorVersion")?.jsonPrimitive?.content)
    }
}
