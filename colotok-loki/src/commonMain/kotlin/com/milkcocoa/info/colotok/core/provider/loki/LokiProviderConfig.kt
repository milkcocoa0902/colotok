package com.milkcocoa.info.colotok.core.provider.loki

import com.milkcocoa.info.colotok.core.formatter.builtin.text.SimpleTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.metrics.MetricsCollectorSpec
import com.milkcocoa.info.colotok.core.provider.details.AsyncProviderConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*

/**
 * Represents authentication credentials for Loki API access.
 */
sealed class Credential{
    /**
     * Basic authentication with username and password.
     *
     * @property username The username for basic authentication
     * @property password The password for basic authentication
     */
    data class Basic(val username: String, val password: String) : Credential()
}

/**
 * Configuration for the Loki log provider.
 * 
 * This class contains all the settings needed to configure a connection to a Loki server
 * for sending log data.
 */
class LokiProviderConfig: AsyncProviderConfig {
    /** The minimum log level that will be sent to Loki */
    override var level: Level = LogLevel.INFO

    /** The formatter used to format log messages */
    override var formatter: Formatter = SimpleTextFormatter

    override var metricsSpec: MetricsCollectorSpec = MetricsCollectorSpec.Inherit
    override var enableInternalMetricsLogging: Boolean = false

    /** The Loki server host URL */
    var host: String? = null

    /** Labels to attach to the log stream */
    var logStream: Map<String, String>? = null

    /** Number of log entries to buffer before sending to Loki */
    override var bufferSize: Int = 50

    private var currentHttpClient: HttpClient? = null
    private var providerOwnsHttpClient: Boolean = false

    internal var httpClientFactory: () -> HttpClient = { HttpClient(CIO) }
    internal var httpClientCloser: (HttpClient) -> Unit = { it.close() }

    /**
     * HTTP client used for API requests.
     *
     * The default client is created lazily and is owned by the provider. A client assigned by
     * the caller remains caller-owned and is never closed by [LokiProvider].
     */
    var httpClient: HttpClient
        get() = currentHttpClient ?: httpClientFactory().also {
            currentHttpClient = it
            providerOwnsHttpClient = true
        }
        set(value) {
            val previous = currentHttpClient
            if (previous !== value && providerOwnsHttpClient) {
                previous?.let(httpClientCloser)
            }
            currentHttpClient = value
            providerOwnsHttpClient = previous === value && providerOwnsHttpClient
        }

    internal fun acquireHttpClient(): LokiHttpClientLease = LokiHttpClientLease(
        client = httpClient,
        providerOwned = providerOwnsHttpClient,
        close = httpClientCloser,
    )

    /** Authentication credentials for Loki API */
    var credential: Credential? = null
}

internal data class LokiHttpClientLease(
    val client: HttpClient,
    val providerOwned: Boolean,
    val close: (HttpClient) -> Unit,
)
