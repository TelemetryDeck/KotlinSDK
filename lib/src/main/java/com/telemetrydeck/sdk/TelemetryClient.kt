package com.telemetrydeck.sdk

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import java.net.URL

internal class TelemetryClientFactory: TelemetryApiClientFactory {
    override fun create(
        apiBaseURL: URL,
        showDebugLogs: Boolean,
        logger: DebugLogger?
    ): TelemetryApiClient {
        return TelemetryClient(
            apiBaseURL,
            showDebugLogs,
            logger
        )
    }
}


/**
 * The HTTP client to communicate with TelemetryDeck's API
 */
internal class TelemetryClient(
    private val apiBaseURL: URL,
    private val showDebugLogs: Boolean,
    private val debugLogger: DebugLogger?
) : TelemetryApiClient {
    private val client: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json()
        }
        if (showDebugLogs && debugLogger != null) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        debugLogger.debug(message)
                    }
                }
                level = LogLevel.ALL // Log request, response, headers, and body
            }
        }

        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }
    }

    override suspend fun send(
        signals: List<Signal>
    ) {
        val response = client.post(getServiceUrl()) {
            setBody(signals)
        }
        println(response.status)
        client.close()
    }

    override fun getServiceUrl(): URL {
        val baseUri = apiBaseURL.toURI()
        val serviceUri = baseUri.resolve("/v2/")
        serviceUri.normalize()
        return serviceUri.toURL()
    }
}