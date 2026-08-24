package com.telemetrydeck.sdk

import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.telemetrydeck.sdk.providers.DisplayParameterProvider
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
class DisplayParameterProviderTest {
    private fun createSut(): DisplayParameterProvider {
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val sut = DisplayParameterProvider()
        sut.register(appContext, TelemetryDeck(configuration = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536"), providers = emptyList()))
        return sut
    }

    @Test
    fun appendsAllExpectedParameters() {
        val sut = createSut()
        val result = sut.enrichTyped("", null)

        assertTrue(result.containsKey("TelemetryDeck.Device.Screens.Primary.width"))
        assertTrue(result.containsKey("TelemetryDeck.Device.Screens.Primary.height"))
        assertTrue(result.containsKey("TelemetryDeck.Device.Screens.Primary.resolution"))
        assertTrue(result.containsKey("TelemetryDeck.Device.Screens.All.width"))
        assertTrue(result.containsKey("TelemetryDeck.Device.Screens.All.height"))
        assertTrue(result.containsKey("TelemetryDeck.Device.Screens.All.resolution"))
        assertTrue(result.containsKey("TelemetryDeck.Device.Screens.All.count"))

        assertTrue(result.getValue("TelemetryDeck.Device.Screens.Primary.width").jsonPrimitive.int > 0)
        assertTrue(result.getValue("TelemetryDeck.Device.Screens.Primary.height").jsonPrimitive.int > 0)
        assertTrue(result.getValue("TelemetryDeck.Device.Screens.Primary.resolution").jsonPrimitive.content.matches(Regex("\\d+,\\d+")))
        assertTrue(result.getValue("TelemetryDeck.Device.Screens.All.resolution").jsonArray.all { it.jsonPrimitive.content.matches(Regex("\\d+,\\d+")) })
        assertTrue(result.getValue("TelemetryDeck.Device.Screens.All.count").jsonPrimitive.int >= 1)

        sut.stop()
    }

    @Test
    fun allDisplayValuesAreAlwaysArraysEvenForASingleDisplay() {
        val sut = createSut()
        val result = sut.enrichTyped("", null)

        val width = result.getValue("TelemetryDeck.Device.Screens.All.width").jsonArray
        assertEquals(result.getValue("TelemetryDeck.Device.Screens.All.count").jsonPrimitive.int, width.size)

        sut.stop()
    }
}
