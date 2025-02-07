package com.telemetrydeck.sdk

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log

class TelemetryDeckInitProvider : ContentProvider() {

    private val tag: String = "TELEMETRYDECK"

    override fun onCreate(): Boolean {
        val appContext = context?.applicationContext
        if (appContext == null || appContext as Application? == null) {
            Log.e(tag, "TelemetryDeckInitProvider requires an Application instance.")
            return false
        }

        try {
            val metadata = ManifestMetadataReader.getConfigurationFromManifest(appContext)
            if (metadata == null) {
                Log.e(
                    tag,
                    "No valid TelemetryDeck SDK configuration found in application manifest."
                )
                return false
            }

            when (metadata.version) {
                TelemetryDeckManifestVersion.V1 -> {
                    throw IllegalArgumentException("This API is no longer supported. Please upgrade to using `TelemetryDeck`.")
                }

                TelemetryDeckManifestVersion.V2 -> {
                    val builder = TelemetryDeck.Builder()
                    builder.configuration(metadata.config)
                    TelemetryDeck.start(appContext, builder)
                }
            }

        } catch (e: Exception) {
            Log.e(tag, "Failed to parse TelemetryDeck SDK configuration:", e)
        }

        return false
    }


    override fun query(
        p0: Uri,
        p1: Array<out String>?,
        p2: String?,
        p3: Array<out String>?,
        p4: String?
    ): Cursor? {
        return null
    }

    override fun getType(p0: Uri): String? {
        return null
    }

    override fun insert(p0: Uri, p1: ContentValues?): Uri? {
        return null
    }

    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int {
        return 0
    }

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
        return 0
    }
}