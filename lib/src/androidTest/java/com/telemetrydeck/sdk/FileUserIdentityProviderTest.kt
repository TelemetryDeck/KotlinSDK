package com.telemetrydeck.sdk

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.telemetrydeck.sdk.providers.FileUserIdentityProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileUserIdentityProviderTest {

    private fun createSut(): FileUserIdentityProvider {
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val sut = FileUserIdentityProvider()
        sut.register(appContext, TelemetryDeck(configuration = TelemetryManagerConfiguration("32CB6574-6732-4238-879F-582FEBEB6536"), providers = emptyList()))
        return sut
    }

    private fun prepareIdentity(value: String) {
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val file = File(appContext.filesDir, "telemetrydeckid")
        file.writeText(value)
    }

    @Test
    fun providesConfigUserIdentity() {
        prepareIdentity("1851B82D-3D39-469D-BED4-19E69C09AF49")

        val sut = createSut()
        val result = sut.calculateIdentity(null, "default user id")
        assertEquals("default user id", result)
    }

    @Test
    fun prefersSignalIdWhenProvided() {
        prepareIdentity("1851B82D-3D39-469D-BED4-19E69C09AF49")

        val sut = createSut()
        val result = sut.calculateIdentity("signal id", "default user id")
        assertEquals("signal id", result)
    }

    @Test
    fun usesDefaultUserIdentityWhenSignalAndConfigIsNull() {
        prepareIdentity("1851B82D-3D39-469D-BED4-19E69C09AF49")

        val sut = createSut()
        val result = sut.calculateIdentity(null, null)
        assertEquals("1851B82D-3D39-469D-BED4-19E69C09AF49", result)
    }

    @Test
    fun createsStableUserIdentity() {
        val sut1 = createSut()
        val first_result = sut1.calculateIdentity(null, null)
        assert(!first_result.isBlank())

        val sut2 = createSut()
        val second_result = sut2.calculateIdentity(null, null)
        assertEquals(first_result, second_result)
    }

    @Test
    fun resetsStableUserIdentity() {
        val sut1 = createSut()
        val first_result = sut1.calculateIdentity(null, null)
        sut1.resetIdentity()

        val sut2 = createSut()
        val second_result = sut2.calculateIdentity(null, null)
        assertNotEquals(first_result, second_result)
    }
}