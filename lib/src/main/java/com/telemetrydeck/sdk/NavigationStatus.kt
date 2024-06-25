package com.telemetrydeck.sdk

interface NavigationStatus {
    /**
     * Apply the provided path as a visited destination.
     */
    fun applyDestination(path: String)
}