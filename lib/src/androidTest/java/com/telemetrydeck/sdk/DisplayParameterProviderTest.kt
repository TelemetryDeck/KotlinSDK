package com.telemetrydeck.sdk

import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.telemetrydeck.sdk.providers.DisplayParameterProvider
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
        val result = sut.enrich("", null)

        assertTrue(result.containsKey("TelemetryDeck.Device.Screens.Primary.width"))
        assertTrue(result.containsKey("TelemetryDeck.Device.Screens.Primary.height"))
        assertTrue(result.containsKey("TelemetryDeck.Device.Screens.Primary.resolution"))
        assertTrue(result.containsKey("TelemetryDeck.Device.Screens.All.width"))
        assertTrue(result.containsKey("TelemetryDeck.Device.Screens.All.height"))
        assertTrue(result.containsKey("TelemetryDeck.Device.Screens.All.resolution"))
        assertTrue(result.containsKey("TelemetryDeck.Device.Screens.All.count"))

        assertTrue(result.getValue("TelemetryDeck.Device.Screens.Primary.width").toInt() > 0)
        assertTrue(result.getValue("TelemetryDeck.Device.Screens.Primary.height").toInt() > 0)
        assertTrue(result.getValue("TelemetryDeck.Device.Screens.Primary.resolution").matches(Regex("\\d+,\\d+")))
        assertTrue(result.getValue("TelemetryDeck.Device.Screens.All.resolution").split(";").all { it.matches(Regex("\\d+,\\d+")) })
        assertTrue(result.getValue("TelemetryDeck.Device.Screens.All.count").toInt() >= 1)

        sut.stop()
    }

    @Test
    fun doesNotOverwriteACallerProvidedValue() {
        val sut = createSut()
        val result = sut.enrich("", null, mapOf("TelemetryDeck.Device.Screens.Primary.width" to "custom"))

        assertEquals("custom", result["TelemetryDeck.Device.Screens.Primary.width"])

        sut.stop()
    }
}
