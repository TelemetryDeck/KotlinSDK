package com.telemetrydeck.sdk.providers

import android.provider.Settings
import com.telemetrydeck.sdk.DebugLogger
import com.telemetrydeck.sdk.TelemetryDeck
import com.telemetrydeck.sdk.TelemetryManagerConfiguration
import com.telemetrydeck.sdk.params.Accessibility
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AccessibilityProviderTest {

    private val errors = mutableListOf<String>()

    private val recordingLogger = object : DebugLogger {
        override fun error(message: String) {
            errors.add(message)
        }
        override fun debug(message: String) {}
        override fun configure(enabled: Boolean) {}
    }

    private fun createSut(): AccessibilityProvider {
        val sut = AccessibilityProvider()
        val manager = TelemetryDeck(
            configuration = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536"),
            providers = emptyList()
        )
        manager.logger = recordingLogger
        sut.register(RuntimeEnvironment.getApplication(), manager)
        return sut
    }

    @Test
    fun reportsMotionAndTransparencyAsDisabledWhenAnimationScalesAreUnset() {
        val contentResolver = RuntimeEnvironment.getApplication().contentResolver
        Assert.assertThrows(Settings.SettingNotFoundException::class.java) {
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE)
        }
        Assert.assertThrows(Settings.SettingNotFoundException::class.java) {
            Settings.Global.getFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE)
        }

        val result = createSut().enrich("", null, emptyMap())

        Assert.assertEquals("false", result[Accessibility.IsReduceMotionEnabled.paramName])
        Assert.assertEquals("false", result[Accessibility.IsReduceTransparencyEnabled.paramName])
        Assert.assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun reportsMotionAndTransparencyAsEnabledWhenAnimationScalesAreZero() {
        val contentResolver = RuntimeEnvironment.getApplication().contentResolver
        Settings.Global.putFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0.0f)
        Settings.Global.putFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0.0f)

        val result = createSut().enrich("", null, emptyMap())

        Assert.assertEquals("true", result[Accessibility.IsReduceMotionEnabled.paramName])
        Assert.assertEquals("true", result[Accessibility.IsReduceTransparencyEnabled.paramName])
        Assert.assertEquals(emptyList<String>(), errors)
    }

    @Test
    fun reportsColorCorrectionAsDisabledWhenDaltonizerSettingsAreUnset() {
        val result = createSut().enrich("", null, emptyMap())

        Assert.assertEquals("false", result[Accessibility.ShouldDifferentiateWithoutColor.paramName])
        Assert.assertEquals(emptyList<String>(), errors)
    }
}
