package com.milkcocoa.info.colotok.core.provider.loki

import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.AsyncProvider
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/**
 * A log provider implementation for Grafana Loki.
 *
 * This provider sends log entries to a Loki server using its HTTP API.
 * It supports buffering log entries and sending them in batches for better performance.
 */
class LokiProvider(config: LokiProviderConfig) : AsyncProvider(validateLokiProviderConfig(config)) {
    /**
     * Convenience constructor that accepts a configuration lambda.
     *
     * @param config Lambda with receiver to configure the provider
     */
    constructor(config: LokiProviderConfig.() -> Unit) : this(LokiProviderConfig().apply(config))

    /**
     * Default constructor that creates a provider with default configuration.
     */
    constructor() : this(LokiProviderConfig())

    // Configuration properties
    private val host = config.host!!.trimEnd('/').plus("/loki/api/v1/push")
    private val logStream = config.logStream!!
    private val httpClientDelegate = lazy { config.acquireHttpClient() }
    private val httpClient: io.ktor.client.HttpClient
        get() = httpClientDelegate.value.client
    private val credential = config.credential

    private fun eventTimestamp(record: LogRecord): Instant =
        when (record) {
            is LogRecord.PlainText -> record.eventTimestamp
            is LogRecord.StructuredText<*> -> record.eventTimestamp
            is LogRecord.Metrics -> record.eventTimestamp
            is LogRecord.Pin -> error("Pin records cannot be published")
        }

    /**
     * Sends the buffered log entries to the Loki server.
     *
     * This method creates a payload with all buffered log entries and sends it to Loki
     * using the configured HTTP client. After successful sending, the buffer is cleared.
     */
    override suspend fun onPublish(records: List<LogRecord>) {
        val response =
            httpClient.post(urlString = host) {
                // Apply authentication if configured
                when (credential) {
                    is Credential.Basic -> {
                        basicAuth(username = credential.username, password = credential.password)
                    }
                    null -> Unit
                }
                contentType(ContentType.Application.Json)

                // Create and serialize the payload
                setBody(
                    Json.encodeToString(
                        LokiPushPayload.serializer(),
                        LokiPushPayload(
                            streams =
                                listOf(
                                    LokiStream(
                                        stream = logStream,
                                        values =
                                            records.map {
                                                LokiValue(
                                                    timestamp = eventTimestamp(it),
                                                    value = it.format(config.formatter)
                                                )
                                            }
                                    )
                                )
                        )
                    )
                )
            }
        response.bodyAsText()
        if (response.status.value !in 200..299) {
            error("Loki publish failed with HTTP ${response.status.value}")
        }
    }

    override fun onClosed() {
        if (!httpClientDelegate.isInitialized()) return
        httpClientDelegate.value.takeIf { it.providerOwned }?.let { lease ->
            lease.close(lease.client)
        }
    }
}

private fun validateLokiProviderConfig(config: LokiProviderConfig): LokiProviderConfig =
    config.apply {
        val configuredHost = checkNotNull(host) { "Loki host URL must be provided" }
        check(configuredHost.isNotBlank()) { "Loki host URL must not be blank" }
        val configuredStream = checkNotNull(logStream) { "Log stream labels must be provided" }
        check(configuredStream.isNotEmpty()) { "Log stream labels must not be empty" }
        check(configuredStream.keys.none { it.isBlank() }) { "Log stream label names must not be blank" }
        val configuredCredential = credential
        if (configuredCredential is Credential.Basic) {
            check(configuredCredential.username.isNotBlank()) { "Loki basic-auth username must not be blank" }
        }
    }