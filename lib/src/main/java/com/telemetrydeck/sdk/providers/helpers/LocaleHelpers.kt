package com.telemetrydeck.sdk.providers.helpers

import android.content.Context
import android.os.Build
import java.util.Locale

/**
 * Retrieves the current language and region set for the application.
 *
 * @param context The application context.
 * @return A [Pair] containing the current language code (e.g., "en") as the first element
 *         and the current region code (e.g., "US") as the second element.
 *
 * Example:
 * ```kotlin
 * val (language, region) = getCurrentAppLanguageAndRegion(applicationContext)
 * println("Current language: $language") // Output: Current language: en (e.g)
 * println("Current region: $region")   // Output: Current region: US (e.g)
 * ```
 *
 * Note: The language and region codes are in the ISO 639-1 and ISO 3166-1 alpha-2 standards, respectively.
 */
internal fun getCurrentAppLanguageAndRegion(context: Context): Pair<String, String> {
    val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales[0]
    } else {
        @Suppress("DEPRECATION")
        context.resources.configuration.locale
    }
    val language: String = locale.language
    val region: String = locale.country
    return Pair(language, region)
}