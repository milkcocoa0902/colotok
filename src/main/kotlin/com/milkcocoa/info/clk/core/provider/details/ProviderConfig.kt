package com.milkcocoa.info.clk.core.provider.details

import com.milkcocoa.info.clk.core.formatter.details.Formatter
import com.milkcocoa.info.clk.core.logger.LogLevel

interface ProviderConfig {
    /**
     * log level
     */
    var logLevel: LogLevel

    var formatter: Formatter

    var colorize: Boolean
}