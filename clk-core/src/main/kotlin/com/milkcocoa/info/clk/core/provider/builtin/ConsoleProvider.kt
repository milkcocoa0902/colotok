package com.milkcocoa.info.clk.core.provider.builtin

import com.milkcocoa.info.clk.core.LogLevel
import com.milkcocoa.info.clk.core.provider.details.Provider
import com.milkcocoa.info.clk.core.provider.details.ProviderColorConfig
import com.milkcocoa.info.clk.core.provider.details.ProviderConfig
import com.milkcocoa.info.clk.util.color.AnsiColor
import com.milkcocoa.info.clk.util.color.Color

class ConsoleProvider(config: ConsoleProviderConfig) : Provider {
    constructor(config: ConsoleProviderConfig.() -> Unit): this(ConsoleProviderConfig().apply(config))

    /**
     * initialize provider with default configuration
     */
    constructor(): this(ConsoleProviderConfig())

    class ConsoleProviderConfig: ProviderConfig, ProviderColorConfig{
        /**
         * log level.
         */
        override var logLevel: LogLevel = LogLevel.DEBUG

        override var traceLevelColor: AnsiColor = AnsiColor.WHITE
        override var debugLevelColor: AnsiColor = AnsiColor.BLUE
        override var infoLevelColor: AnsiColor = AnsiColor.GREEN
        override var warnLevelColor: AnsiColor = AnsiColor.YELLOW
        override var errorLevelColor: AnsiColor = AnsiColor.RED
    }

    private val logLevel = config.logLevel
    private val getColor: ((LogLevel) -> AnsiColor? ) = {
        config.getColorForLevel(it)
    }
    override fun write(name: String, str: String, level: LogLevel) {
        if(level.isEnabledFor(logLevel).not()){
            return
        }


        getColor(level)?.let {
            println(Color.foreground(formatter.format(str, level), it))
        } ?: run {
            println(str)
        }
    }
}