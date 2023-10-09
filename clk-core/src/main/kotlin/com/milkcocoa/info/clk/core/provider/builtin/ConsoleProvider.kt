package com.milkcocoa.info.clk.core.provider.builtin

import com.milkcocoa.info.clk.core.LogLevel
import com.milkcocoa.info.clk.core.provider.details.Provider
import com.milkcocoa.info.clk.util.color.Color

class ConsoleProvider(config: ConsoleProviderConfig) : Provider {
    constructor(config: ConsoleProviderConfig.() -> Unit): this(ConsoleProviderConfig().apply(config))

    /**
     * initialize provider with default configuration
     */
    constructor(): this(ConsoleProviderConfig())

    class ConsoleProviderConfig{
        /**
         * log level.
         */
        var logLevel: LogLevel = LogLevel.DEBUG
    }

    private val logLevel = config.logLevel
    override fun write(name: String, str: String, level: LogLevel) {
        if(level.isEnabledFor(logLevel).not()){
            return
        }

        colors.get(level)?.let {
            println(Color.foreground(formatter.format(str, level), it))
        } ?: run {
            println(str)
        }
    }
}