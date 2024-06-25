package com.telemetrydeck.sdk

class MemoryNavigationStatus(private var previousNavigationPath: String? = null) :
    NavigationStatus {

    /**
     * Apply the provided path as a visited destination.
     */
    override fun applyDestination(path: String) {
        previousNavigationPath = path
    }

    override fun getLastDestination(): String {
        return previousNavigationPath ?: ""
    }
}