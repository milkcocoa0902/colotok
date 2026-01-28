package com.milkcocoa.info.colotok.core.provider.loki

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
class LokiProvider(config: LokiProviderConfig): AsyncProvider() {
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
    private val logLevel = config.level
    private val formatter = config.formatter
    private val host = config.host?.trimEnd('/')?.plus("/loki/api/v1/push") ?: error("Loki host URL must be provided")
    private val logStream =  config.logStream ?: error("Log stream labels must be provided")
    private val bufferSize = config.bufferSize
    private val httpClient = config.httpClient
    private val credential = config.credential

    // Buffer for storing log entries before sending them to Loki
    private val buffer = mutableListOf<LokiValue>()
    private val mutex = Mutex()


    /**
     * Sends the buffered log entries to the Loki server.
     * 
     * This method creates a payload with all buffered log entries and sends it to Loki
     * using the configured HTTP client. After successful sending, the buffer is cleared.
     */
    private suspend fun sendLogsToLoki(){
        runCatching {
            mutex.withLock {
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
                                    values = buffer
                                )
                            )
                        )
                    ))
                }
                // Clear the buffer after successful sending
                buffer.clear()
            }
        }
    }


    /**
     * Asynchronously writes a string log message to Loki.
     * 
     * @param name The logger name
     * @param msg The log message
     * @param level The log level
     * @param attr Additional attributes to include with the log
     */
    @OptIn(ExperimentalTime::class)
    override suspend fun writeAsync(record: LogRecord) {
        if(record.level.isEnabledFor(logLevel).not()) return

        runCatching {
            // Add the log entry to the buffer and check if we should send logs
            val shouldSendLogs = mutex.withLock {
                buffer.add(
                    LokiValue(
                        timestamp = Clock.System.now(),
                        value = record.format(formatter)
                    )
                )

                // Return true if buffer has reached the configured size
                buffer.size >= bufferSize
            }

            // Send logs if the buffer is full
            if(shouldSendLogs){
                sendLogsToLoki()
            }
        }
    }

    /**
     * Flushes any buffered log entries to Loki immediately.
     * 
     * This method can be called to ensure all logs are sent to Loki,
     * for example before application shutdown.
     */
    suspend fun flush(){
        sendLogsToLoki()
    }
}
