package com.telemetrydeck.sdk.providers

import android.os.Looper
import com.telemetrydeck.sdk.MemorySignalCache
import com.telemetrydeck.sdk.TelemetryDeck
import com.telemetrydeck.sdk.TelemetryManagerConfiguration
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
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
        val result = sut.enrichTyped("", null)

        Assert.assertTrue(result.containsKey("TelemetryDeck.Device.Screens.Primary.width"))
        Assert.assertTrue(result.containsKey("TelemetryDeck.Device.Screens.Primary.height"))
        Assert.assertTrue(result.containsKey("TelemetryDeck.Device.Screens.Primary.resolution"))
        Assert.assertTrue(result.containsKey("TelemetryDeck.Device.Screens.All.width"))
        Assert.assertTrue(result.containsKey("TelemetryDeck.Device.Screens.All.height"))
        Assert.assertTrue(result.containsKey("TelemetryDeck.Device.Screens.All.resolution"))
        Assert.assertTrue(result.containsKey("TelemetryDeck.Device.Screens.All.count"))

        Assert.assertTrue(result.getValue("TelemetryDeck.Device.Screens.Primary.width").jsonPrimitive.int > 0)
        Assert.assertTrue(result.getValue("TelemetryDeck.Device.Screens.Primary.height").jsonPrimitive.int > 0)
        Assert.assertTrue(result.getValue("TelemetryDeck.Device.Screens.Primary.resolution").jsonPrimitive.content.matches(Regex("\\d+,\\d+")))
        Assert.assertTrue(result.getValue("TelemetryDeck.Device.Screens.All.count").jsonPrimitive.int >= 1)

        sut.stop()
    }

    @Test
    fun allDisplayValuesAreAlwaysArraysEvenForASingleDisplay() {
        val sut = createSut()
        val result = sut.enrichTyped("", null)

        val width = result.getValue("TelemetryDeck.Device.Screens.All.width").jsonArray
        Assert.assertEquals(1, width.size)
        Assert.assertTrue(width.first().jsonPrimitive.int > 0)

        val resolution = result.getValue("TelemetryDeck.Device.Screens.All.resolution").jsonArray
        Assert.assertEquals(1, resolution.size)
        Assert.assertTrue(resolution.first().jsonPrimitive.content.matches(Regex("\\d+,\\d+")))

        sut.stop()
    }

    @Test
    fun joinsMultipleDisplaysIntoArrays() {
        val sut = createSut()
        val displayId = ShadowDisplayManager.addDisplay("w1080dp-h1920dp")
        shadowOf(Looper.getMainLooper()).idle()

        val result = sut.enrichTyped("", null)

        Assert.assertEquals(JsonPrimitive(2), result["TelemetryDeck.Device.Screens.All.count"])
        Assert.assertEquals(2, result.getValue("TelemetryDeck.Device.Screens.All.width").jsonArray.size)
        Assert.assertTrue(result.getValue("TelemetryDeck.Device.Screens.All.resolution").jsonArray.all { it.jsonPrimitive.content.matches(Regex("\\d+,\\d+")) })

        ShadowDisplayManager.removeDisplay(displayId)
        sut.stop()
    }

    @Test
    fun invalidatesCacheWhenADisplayIsAddedOrRemoved() {
        val sut = createSut()
        val initialCount = sut.enrichTyped("", null).getValue("TelemetryDeck.Device.Screens.All.count").jsonPrimitive.int

        val displayId = ShadowDisplayManager.addDisplay("w1080dp-h1920dp")
        shadowOf(Looper.getMainLooper()).idle()
        val countAfterAdd = sut.enrichTyped("", null).getValue("TelemetryDeck.Device.Screens.All.count").jsonPrimitive.int

        ShadowDisplayManager.removeDisplay(displayId)
        shadowOf(Looper.getMainLooper()).idle()
        val countAfterRemove = sut.enrichTyped("", null).getValue("TelemetryDeck.Device.Screens.All.count").jsonPrimitive.int

        Assert.assertEquals(1, initialCount)
        Assert.assertEquals(2, countAfterAdd)
        Assert.assertEquals(1, countAfterRemove)

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

    @Test
    fun callerProvidedStringParamWinsOverTypedParam() {
        val context = RuntimeEnvironment.getApplication()
        val sut = DisplayParameterProvider()
        val configuration = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536")
        val manager = TelemetryDeck(configuration = configuration, providers = listOf(sut))
        manager.cache = MemorySignalCache()
        sut.register(context, manager)

        manager.signal("test", params = mapOf("TelemetryDeck.Device.Screens.Primary.width" to "custom"))

        val signal = manager.cache!!.empty().first()
        Assert.assertEquals(JsonPrimitive("custom"), signal.payload["TelemetryDeck.Device.Screens.Primary.width"])

        sut.stop()
    }
}
