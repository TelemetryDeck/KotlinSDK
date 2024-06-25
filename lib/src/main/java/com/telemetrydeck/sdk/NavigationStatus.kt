package com.telemetrydeck.sdk

interface NavigationStatus {
    /**
     * Apply the provided path as a visited destination.
     */
    fun applyDestination(path: String)

    /**
     * Returns the last destination path or an empty string if none has been provided.
     */
    fun getLastDestination(): String
}