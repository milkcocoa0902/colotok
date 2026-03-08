package com.milkcocoa.info.colotok.core.provider.loki

import com.milkcocoa.info.colotok.core.coroutines.blocking
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.AsyncProvider
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * A log provider implementation for Grafana Loki.
 * 
 * This provider sends log entries to a Loki server using its HTTP API.
 * It supports buffering log entries and sending them in batches for better performance.
 */
class LokiProvider(config: LokiProviderConfig): AsyncProvider(config) {
    /**
     * Convenience constructor that accepts a configuration lambda.
     * 
     * @param config Lambda with receiver to configure the provider
     */
    constructor(config: LokiProviderConfig.()->Unit): this(LokiProviderConfig().apply(config))

    /**
     * Default constructor that creates a provider with default configuration.
     */
    constructor(): this(LokiProviderConfig())

    // Configuration properties
    private val host = config.host?.trimEnd('/')?.plus("/loki/api/v1/push") ?: error("Loki host URL must be provided")
    private val logStream =  config.logStream ?: error("Log stream labels must be provided")
    private val httpClient = config.httpClient
    private val credential = config.credential


    /**
     * Sends the buffered log entries to the Loki server.
     * 
     * This method creates a payload with all buffered log entries and sends it to Loki
     * using the configured HTTP client. After successful sending, the buffer is cleared.
     */
    @OptIn(ExperimentalTime::class)
    override suspend fun onPublish(records: List<LogRecord>) {
        runCatching {
            httpClient.post(urlString = host){
                // Apply authentication if configured
                when(credential){
                    is Credential.Basic ->{
                        basicAuth(username =  credential.username, password = credential.password)
                    }
                    null -> Unit
                }
                contentType(ContentType.Application.Json)

                // Create and serialize the payload
                setBody(Json.encodeToString(
                    LokiPushPayload.serializer(),
                    LokiPushPayload(
                        streams = listOf(
                            LokiStream(
                                stream = logStream,
                                values = records.map {
                                    LokiValue(
                                        timestamp = Clock.System.now(),
                                        value = it.format(config.formatter)
                                    )
                                }
                            )
                        )
                    )
                ))
            }
        }
    }


    override fun onClosed() {}
}
