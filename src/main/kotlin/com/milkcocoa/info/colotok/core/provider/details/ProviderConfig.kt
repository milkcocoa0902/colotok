package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.logger.LogLevel

interface ProviderConfig {
    /**
     * log level
     */
    var logLevel: LogLevel

    var formatter: Formatter

    var colorize: Boolean
}