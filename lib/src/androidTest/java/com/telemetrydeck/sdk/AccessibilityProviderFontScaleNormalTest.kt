package com.telemetrydeck.sdk

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.telemetrydeck.sdk.providers.AccessibilityProvider
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityProviderFontScaleNormalTest() {
    private fun createSut(): AccessibilityProvider {
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val sut = AccessibilityProvider()
        sut.register(appContext,
            TelemetryDeck(
                configuration = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536"),
                providers = emptyList()
            )
        )
        return sut
    }

    @get:Rule
    val accessibilitySettingsRule = FontScaleTestRule(1.0f)

    @Test
    fun fontScaleMappingTest() {
        val sut = createSut()
        val result = sut.enrich("", null)
        Assert.assertEquals("L", result["TelemetryDeck.Accessibility.fontScale"])
    }
}