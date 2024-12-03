package com.telemetrydeck.sdk.platform

internal data class AppInstallationInfo(
    val packageName: String,
    val isSideLoaded: Boolean,
    val sourceMarketPlace: String?
)