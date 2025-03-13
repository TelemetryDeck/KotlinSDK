package com.telemetrydeck.sdk


import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.telemetrydeck.sdk.providers.AccessibilityProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityProviderEnrichmentParametersTest {
    private fun createSut(): AccessibilityProvider {
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val sut = AccessibilityProvider()
        sut.register(appContext, TelemetryDeck(configuration = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536"), providers = emptyList()))
        return sut
    }

    @Test
    fun appendsAllExpectedParameters() {
        val sut = createSut()
        val result = sut.enrich("", null)
        assertTrue(result.containsKey("TelemetryDeck.Accessibility.fontWeightAdjustment"))
        assertTrue(result.containsKey("TelemetryDeck.Accessibility.isBoldTextEnabled"))
        assertTrue(result.containsKey("TelemetryDeck.Accessibility.isDarkerSystemColorsEnabled"))
        assertTrue(result.containsKey("TelemetryDeck.Accessibility.fontScale"))
        assertTrue(result.containsKey("TelemetryDeck.Accessibility.isInvertColorsEnabled"))
        assertTrue(result.containsKey("TelemetryDeck.Accessibility.shouldDifferentiateWithoutColor"))
        assertTrue(result.containsKey("TelemetryDeck.Accessibility.isReduceMotionEnabled"))
        assertTrue(result.containsKey("TelemetryDeck.Accessibility.isReduceTransparencyEnabled"))
        assertTrue(result.containsKey("TelemetryDeck.UserPreference.layoutDirection"))
        assertTrue(result.containsKey("TelemetryDeck.UserPreference.region"))
        assertTrue(result.containsKey("TelemetryDeck.UserPreference.language"))
        assertTrue(result.containsKey("TelemetryDeck.UserPreference.colorScheme"))
    }
}
