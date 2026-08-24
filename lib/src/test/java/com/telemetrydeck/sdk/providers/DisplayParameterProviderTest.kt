package com.telemetrydeck.sdk.providers

import android.os.Looper
import com.telemetrydeck.sdk.TelemetryDeck
import com.telemetrydeck.sdk.TelemetryManagerConfiguration
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplayManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DisplayParameterProviderTest {
    private fun createSut(now: () -> Long = System::currentTimeMillis, testMode: Boolean = false): DisplayParameterProvider {
        val context = RuntimeEnvironment.getApplication()
        val sut = DisplayParameterProvider(now)
        val configuration = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536").apply { this.testMode = testMode }
        sut.register(context, TelemetryDeck(configuration = configuration, providers = emptyList()))
        return sut
    }

    @Test
    fun appendsAllExpectedParameters() {
        val sut = createSut()
        val result = sut.enrich("", null)

        Assert.assertTrue(result.containsKey("TelemetryDeck.Device.Screens.Primary.width"))
        Assert.assertTrue(result.containsKey("TelemetryDeck.Device.Screens.Primary.height"))
        Assert.assertTrue(result.containsKey("TelemetryDeck.Device.Screens.Primary.resolution"))
        Assert.assertTrue(result.containsKey("TelemetryDeck.Device.Screens.All.width"))
        Assert.assertTrue(result.containsKey("TelemetryDeck.Device.Screens.All.height"))
        Assert.assertTrue(result.containsKey("TelemetryDeck.Device.Screens.All.resolution"))
        Assert.assertTrue(result.containsKey("TelemetryDeck.Device.Screens.All.count"))

        Assert.assertTrue(result.getValue("TelemetryDeck.Device.Screens.Primary.width").toInt() > 0)
        Assert.assertTrue(result.getValue("TelemetryDeck.Device.Screens.Primary.height").toInt() > 0)
        Assert.assertTrue(result.getValue("TelemetryDeck.Device.Screens.Primary.resolution").matches(Regex("\\d+,\\d+")))
        Assert.assertTrue(result.getValue("TelemetryDeck.Device.Screens.All.count").toInt() >= 1)

        sut.stop()
    }

    @Test
    fun doesNotOverwriteACallerProvidedValue() {
        val sut = createSut()
        val result = sut.enrich("", null, mapOf("TelemetryDeck.Device.Screens.Primary.width" to "custom"))

        Assert.assertEquals("custom", result["TelemetryDeck.Device.Screens.Primary.width"])

        sut.stop()
    }

    @Test
    fun joinsValuesWithSemicolonsWhenMultipleDisplaysAreConnected() {
        val sut = createSut()
        val displayId = ShadowDisplayManager.addDisplay("w1080dp-h1920dp")
        shadowOf(Looper.getMainLooper()).idle()

        val result = sut.enrich("", null)

        Assert.assertEquals("2", result["TelemetryDeck.Device.Screens.All.count"])
        Assert.assertTrue(result.getValue("TelemetryDeck.Device.Screens.All.width").contains(";"))
        Assert.assertTrue(result.getValue("TelemetryDeck.Device.Screens.All.resolution").split(";").all { it.matches(Regex("\\d+,\\d+")) })

        ShadowDisplayManager.removeDisplay(displayId)
        sut.stop()
    }

    @Test
    fun invalidatesCacheWhenADisplayIsAddedOrRemoved() {
        val sut = createSut()
        val initialCount = sut.enrich("", null).getValue("TelemetryDeck.Device.Screens.All.count")

        val displayId = ShadowDisplayManager.addDisplay("w1080dp-h1920dp")
        shadowOf(Looper.getMainLooper()).idle()
        val countAfterAdd = sut.enrich("", null).getValue("TelemetryDeck.Device.Screens.All.count")

        ShadowDisplayManager.removeDisplay(displayId)
        shadowOf(Looper.getMainLooper()).idle()
        val countAfterRemove = sut.enrich("", null).getValue("TelemetryDeck.Device.Screens.All.count")

        Assert.assertEquals("1", initialCount)
        Assert.assertEquals("2", countAfterAdd)
        Assert.assertEquals("1", countAfterRemove)

        sut.stop()
    }

    @Test
    fun servesCachedParamsUntilCacheLifetimeElapsesThenRefreshes() {
        var currentTime = 0L
        val sut = createSut(now = { currentTime })
        val initial = sut.resolvedParams()

        currentTime += DisplayParameterProvider.CACHE_LIFETIME_MILLIS - 1
        val beforeExpiry = sut.resolvedParams()

        currentTime += 2
        val afterExpiry = sut.resolvedParams()

        Assert.assertSame(initial, beforeExpiry)
        Assert.assertNotSame(initial, afterExpiry)

        sut.stop()
    }

    @Test
    fun bypassesCacheWhenTestModeIsEnabled() {
        val sut = createSut(testMode = true)
        val first = sut.resolvedParams()
        val second = sut.resolvedParams()

        Assert.assertNotSame(first, second)

        sut.stop()
    }
}
