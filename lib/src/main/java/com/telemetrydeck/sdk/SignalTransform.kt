package com.telemetrydeck.sdk

data class SignalTransform(val signalType: String, val clientUser: String?, val additionalPayload: Map<String, String>, val floatValue: Double?)