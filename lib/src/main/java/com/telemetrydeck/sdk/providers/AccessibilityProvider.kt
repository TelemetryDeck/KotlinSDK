package com.telemetrydeck.sdk.providers

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.util.LayoutDirection
import android.view.accessibility.AccessibilityManager
import androidx.core.text.layoutDirection
import com.telemetrydeck.sdk.TelemetryDeckProvider
import com.telemetrydeck.sdk.TelemetryDeckSignalProcessor
import com.telemetrydeck.sdk.params.Accessibility
import com.telemetrydeck.sdk.params.Device
import com.telemetrydeck.sdk.params.UserPreferences
import java.lang.ref.WeakReference
import java.util.Locale


class AccessibilityProvider : TelemetryDeckProvider {
    private var app: WeakReference<Application?>? = null
    private var manager: WeakReference<TelemetryDeckSignalProcessor>? = null

    override fun register(ctx: Application?, client: TelemetryDeckSignalProcessor) {
        this.app = WeakReference(ctx)
        this.manager = WeakReference(client)
    }

    override fun stop() {

    }

    override fun enrich(
        signalType: String,
        clientUser: String?,
        additionalPayload: Map<String, String>
    ): Map<String, String> {
        val signalPayload = additionalPayload.toMutableMap()
        for (item in getConfigurationParams()) {
            if (!signalPayload.containsKey(item.key)) {
                signalPayload[item.key] = item.value
            }
        }
        return signalPayload
    }

    private fun getConfigurationParams(): Map<String, String> {

        val context = this.app?.get()?.applicationContext ?: return emptyMap()
        val config = context.resources.configuration
        val attributes = mutableMapOf<String, String>()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                attributes[Accessibility.FontWeightAdjustment.paramName] =
                    "${config.fontWeightAdjustment}"
                attributes[Accessibility.IsBoldTextEnabled.paramName] =
                    "${config.fontWeightAdjustment > 0}"
            }
        } catch (e: Exception) {
            this.manager?.get()?.debugLogger?.error("Error detecting FontWeightAdjustment: ${e.stackTraceToString()}")
        }


        try {
            isDarkModeEnabled().let {
                attributes[Accessibility.IsDarkerSystemColorsEnabled.paramName] = "$it"
            }
        } catch (e: Exception) {
            this.manager?.get()?.debugLogger?.error("Error detecting IsDarkerSystemColorsEnabled: ${e.stackTraceToString()}")
        }

        try {
            attributes[Accessibility.FontScale.paramName] = "${config.fontScale}"
        } catch (e: Exception) {
            this.manager?.get()?.debugLogger?.error("Error detecting FontScale: ${e.stackTraceToString()}")
        }

        try {
            val isColorInversionEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED,
                0 // Default value if the setting is not found
            ) == 1
            attributes[Accessibility.IsInvertColorsEnabled.paramName] = "$isColorInversionEnabled"
        } catch (e: Exception) {
            this.manager?.get()?.debugLogger?.error("Error detecting IsInvertColorsEnabled: ${e.stackTraceToString()}")
        }

        try {
            val colorCorrectionSettingKey = "accessibility_display_daltonizer_enabled"
            val colorModeSettingKey = "accessibility_display_daltonizer"
            val isColorCorrectionEnabled =
                Settings.Secure.getInt(context.contentResolver, colorCorrectionSettingKey) == 1 &&
                        Settings.Secure.getInt(context.contentResolver, colorModeSettingKey) == 0
            attributes[Accessibility.ShouldDifferentiateWithoutColor.paramName] =
                "$isColorCorrectionEnabled"
        } catch (e: Settings.SettingNotFoundException) {
            attributes[Accessibility.ShouldDifferentiateWithoutColor.paramName] = "false"
        } catch (e: Exception) {
            this.manager?.get()?.debugLogger?.error("Error detecting ShouldDifferentiateWithoutColor: ${e.stackTraceToString()}")
        }

        try {
            val transitionAnimationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE
            )

            val animatorDurationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE
            )

            attributes[Accessibility.IsReduceMotionEnabled.paramName] =
                "${transitionAnimationScale == 0.0f && animatorDurationScale == 0.0f}"
            attributes[Accessibility.IsReduceTransparencyEnabled.paramName] =
                "${transitionAnimationScale == 0.0f}"
        } catch (e: Exception) {
            this.manager?.get()?.debugLogger?.error("Error detecting IsReduceMotionEnabled: ${e.stackTraceToString()}")
        }

        try {
            attributes[UserPreferences.LayoutDirection.paramName] =
                when (Locale.getDefault().layoutDirection == LayoutDirection.RTL) {
                    true -> "rightToLeft"
                    false -> "leftToRight"
                }
        } catch (e: Exception) {
            this.manager?.get()?.debugLogger?.error("Error detecting LayoutDirection: ${e.stackTraceToString()}")
        }

        return attributes

    }

    private fun isDarkModeEnabled(): Boolean? {
        val context = this.app?.get()?.applicationContext ?: return null
        val nightModeFlags: Int =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> {
                return true
            }

            Configuration.UI_MODE_NIGHT_NO -> {
                return false
            }

            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                return null
            }
        }

        return null
    }
}

