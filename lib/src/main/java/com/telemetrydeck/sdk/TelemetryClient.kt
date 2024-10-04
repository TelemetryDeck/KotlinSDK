package com.telemetrydeck.sdk

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import java.net.URL
import java.util.*

/**
 * The HTTP client to communicate with TelemetryDeck's API
 */
internal class TelemetryClient(
    private val telemetryAppID: UUID,
    private val apiBaseURL: URL,
    private val showDebugLogs: Boolean,
    private val debugLogger: DebugLogger?
) {
    private val client: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json()
        }
        if (showDebugLogs && debugLogger != null) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
        }
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }
    }

    suspend fun send(
        signals: List<Signal>
    ) {
        val response = client.post(getServiceUrl()) {
            setBody(signals)
        }
        println(response.status)
        client.close()
    }

    fun getServiceUrl(): URL {
        val baseUri = apiBaseURL.toURI()
        val serviceUri = baseUri.resolve("/api/v1/apps/${telemetryAppID}/signals/multiple/")
        serviceUri.normalize()
        return serviceUri.toURL()
    }
}