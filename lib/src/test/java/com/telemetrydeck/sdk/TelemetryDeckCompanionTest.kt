package com.telemetrydeck.sdk

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TelemetryDeckCompanionTest {

    @Before
    fun setUp() {
        TelemetryDeck.instance = null
    }

    @After
    fun tearDown() {
        TelemetryDeck.instance = null
    }

    @Test
    fun companion_signal_before_init_does_not_throw() {
        Assert.assertNull(TelemetryDeck.instance)
        TelemetryDeck.signal("foo")
    }
}
