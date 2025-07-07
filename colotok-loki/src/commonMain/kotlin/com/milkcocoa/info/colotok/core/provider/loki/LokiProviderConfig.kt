package com.milkcocoa.info.colotok.core.provider.loki

import com.milkcocoa.info.colotok.core.formatter.builtin.text.SimpleTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.ProviderConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

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
class LokiProviderConfig: ProviderConfig {
    /** The minimum log level that will be sent to Loki */
    override var level: Level = LogLevel.INFO

    /** The formatter used to format log messages */
    override var formatter: Formatter = SimpleTextFormatter

    /** The Loki server host URL */
    var host: String? = null

    /** Labels to attach to the log stream */
    var logStream: Map<String, String>? = null

    /** Number of log entries to buffer before sending to Loki */
    var bufferSize: Int = 50

    /** HTTP client used for API requests */
    var httpClient: HttpClient = HttpClient(CIO)

    /** Authentication credentials for Loki API */
    var credential: Credential? = null
}
