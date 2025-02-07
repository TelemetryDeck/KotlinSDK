package com.telemetrydeck.sdk.providers

import android.content.Context
import android.app.Application
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
    private var app: WeakReference<Context?>? = null
    private var manager: WeakReference<TelemetryDeckSignalProcessor>? = null

    override fun register(ctx: Context?, client: TelemetryDeckSignalProcessor) {
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

        val context = this.app?.get() ?: return emptyMap()
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
            attributes[Accessibility.FontScale.paramName] = mapFontScaleToTelemetryValue(config.fontScale).scale
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
        val context = this.app?.get() ?: return null
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


    fun mapFontScaleToTelemetryValue(fontScale: Float): TelemetryValue {
        // Note: font scale of 1.0 maps to TelemetryValue.L to match the scale in the SwiftSDK
        when {
            fontScale <= 0.8f -> return TelemetryValue.XS
            fontScale > 0.8f && fontScale < 0.9f -> return TelemetryValue.S
            fontScale >= 0.9f && fontScale < 1.0f -> return TelemetryValue.M
            fontScale == 1.0f -> return TelemetryValue.L
            fontScale >= 1.0f && fontScale < 1.3f -> return TelemetryValue.XL
            fontScale >= 1.3f && fontScale < 1.4f -> return TelemetryValue.XXL
            fontScale >= 1.4f && fontScale < 1.5f -> return TelemetryValue.XXXL
            fontScale >= 1.5f && fontScale < 1.6f -> return TelemetryValue.AccessibilityM
            fontScale >= 1.6f && fontScale < 1.7f -> return TelemetryValue.AccessibilityL
            fontScale >= 1.7f && fontScale < 1.8f -> return TelemetryValue.AccessibilityXL
            fontScale >= 1.8f && fontScale < 1.9f -> return TelemetryValue.AccessibilityXXL
            fontScale >= 1.9f && fontScale < 2.0f -> return TelemetryValue.AccessibilityXXXL
            fontScale >= 2.0f -> return TelemetryValue.AccessibilityXXXL
            else -> return TelemetryValue.Unspecified
        }
    }

    enum class TelemetryValue(val scale: String) {
        Unspecified("unspecified"),
        XS("XS"),
        S("S"),
        M("M"),
        L("L"),
        XL("XL"),
        XXL("XXL"),
        XXXL("XXXL"),
        AccessibilityM("AccessibilityM"),
        AccessibilityL("AccessibilityL"),
        AccessibilityXL("AccessibilityXL"),
        AccessibilityXXL("AccessibilityXXL"),
        AccessibilityXXXL("AccessibilityXXXL"),
    }
}

