package com.telemetrydeck.sdk

class MemoryNavigationStatus(private var previousNavigationPath: String? = null) :
    NavigationStatus {

    /**
     * Apply the provided path as a visited destination.
     */
    override fun applyDestination(path: String) {
        previousNavigationPath = path
    }

    /**
     * Returns the last destination path or an empty string if none has been provided.
     */
    override fun getLastDestination(): String {
        return previousNavigationPath ?: ""
    }
}