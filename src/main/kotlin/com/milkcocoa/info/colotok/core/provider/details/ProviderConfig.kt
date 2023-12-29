package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.logger.LogLevel

/**
 * provider's basic config
 */
interface ProviderConfig {
    /**
     * providers log level
     */
    var logLevel: LogLevel

    /**
     * formatter
     */
    var formatter: Formatter

    /**
     * do colorize. if true, provider can print message with ANSI color code.
     */
    var colorize: Boolean
}