package com.telemetrydeck.sdk

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class FontScaleTestRule (private val fontScale: Float) : TestRule {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val uiDevice: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    setupAccessibilityOptions()
                    base.evaluate()
                } finally {
                    cleanupAccessibilityOptions()
                }
            }
        }
    }

    private fun setupAccessibilityOptions() {
        enableLargeText()
    }

    private fun cleanupAccessibilityOptions() {
       disableLargeText()
    }

    private fun enableLargeText() {
        executeShellCommand("settings put system font_scale $fontScale")
        waitForFontScaleChange(fontScale)
    }

    private fun disableLargeText() {
        executeShellCommand("settings put system font_scale 1.0")
        waitForFontScaleChange(1.0f)
    }

    private fun waitForFontScaleChange(expectedScale: Float, timeoutMillis: Long = 2000) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            val currentScale = Settings.System.getFloat(
                context.contentResolver,
                Settings.System.FONT_SCALE,
                1.0f
            )
            if (currentScale == expectedScale) {
                return
            }
            Thread.sleep(100) // Wait for 100 milliseconds before checking again
        }
        throw AssertionError("Font scale did not change to $expectedScale within timeout")
    }

    private fun executeShellCommand(command: String) {
        uiDevice.executeShellCommand(command)
    }
}