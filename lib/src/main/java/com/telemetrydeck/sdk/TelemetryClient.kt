package com.telemetrydeck.sdk

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.net.URL
import java.util.*

/**
 * The HTTP client to communicate with TelemetryDeck's API
 */
internal class TelemetryClient(private val telemetryAppID: UUID, private val apiBaseURL: URL, private val showDebugLogs: Boolean, private val debugLogger: DebugLogger?) {
    private val client: HttpClient = HttpClient(CIO) {
        install(JsonFeature)
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
        val response: HttpResponse = client.request {
            method = HttpMethod.Post
            url(getServiceUrl())
            body = signals
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