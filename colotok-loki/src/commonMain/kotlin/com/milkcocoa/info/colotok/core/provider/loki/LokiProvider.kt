package com.milkcocoa.info.colotok.core.provider.loki

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.provider.details.AsyncProvider
import io.ktor.client.request.basicAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * A log provider implementation for Grafana Loki.
 * 
 * This provider sends log entries to a Loki server using its HTTP API.
 * It supports buffering log entries and sending them in batches for better performance.
 */
class LokiProvider(config: LokiProviderConfig): AsyncProvider {
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
    override suspend fun writeAsync(
        name: String,
        msg: String,
        level: Level,
        attr: Map<String, String>
    ) {
        // Skip if the log level is not enabled
        if (level.isEnabledFor(logLevel).not()) {
            return
        }

        runCatching {
            // Add the log entry to the buffer and check if we should send logs
            val shouldSendLogs = mutex.withLock {
                buffer.add(
                    LokiValue(
                        timestamp = Clock.System.now(),
                        value = formatter.format(
                            msg = msg,
                            level = level,
                            attrs = attr
                        )
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
     * Asynchronously writes a structured log message to Loki.
     * 
     * @param name The logger name
     * @param msg The structured log message
     * @param serializer The serializer for the structured log message
     * @param level The log level
     * @param attr Additional attributes to include with the log
     */
    @OptIn(ExperimentalTime::class)
    override suspend fun <T : LogStructure> writeAsync(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attr: Map<String, String>
    ) {
        // Skip if the log level is not enabled
        if (level.isEnabledFor(logLevel).not()) {
            return
        }

        runCatching {
            // Add the log entry to the buffer and check if we should send logs
            val shouldSendLogs = mutex.withLock {
                buffer.add(
                    LokiValue(
                        timestamp = Clock.System.now(),
                        value = formatter.format(
                            msg = msg,
                            serializer = serializer,
                            level = level,
                            attrs = attr
                        )
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
     * Synchronously writes a string log message to Loki.
     * 
     * This method launches a coroutine to perform the actual writing asynchronously.
     * 
     * @param name The logger name
     * @param msg The log message
     * @param level The log level
     * @param attr Additional attributes to include with the log
     */
    override fun write(
        name: String,
        msg: String,
        level: Level,
        attr: Map<String, String>
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            writeAsync(
                name = name,
                msg = msg,
                level = level,
                attr = attr
            )
        }
    }

    /**
     * Synchronously writes a structured log message to Loki.
     * 
     * This method launches a coroutine to perform the actual writing asynchronously.
     * 
     * @param name The logger name
     * @param msg The structured log message
     * @param serializer The serializer for the structured log message
     * @param level The log level
     * @param attr Additional attributes to include with the log
     */
    override fun <T : LogStructure> write(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attr: Map<String, String>
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            writeAsync(
                name = name,
                msg = msg,
                serializer = serializer,
                level = level,
                attr = attr
            )
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
