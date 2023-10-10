package com.milkcocoa.info.clk.core.provider.builtin

import com.milkcocoa.info.clk.core.logger.LogLevel
import com.milkcocoa.info.clk.core.formatter.builtin.DetailFormatter
import com.milkcocoa.info.clk.core.formatter.details.Formatter
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
        override var formatter: Formatter = DetailFormatter
        override var colorize: Boolean = true

        override var traceLevelColor: AnsiColor = AnsiColor.WHITE
        override var debugLevelColor: AnsiColor = AnsiColor.BLUE
        override var infoLevelColor: AnsiColor = AnsiColor.GREEN
        override var warnLevelColor: AnsiColor = AnsiColor.YELLOW
        override var errorLevelColor: AnsiColor = AnsiColor.RED
    }

    private val logLevel = config.logLevel
    private val formatter = config.formatter
    private var colorize = config.colorize
    private val getColor: ((LogLevel) -> AnsiColor? ) = {
        config.getColorForLevel(it)
    }
    override fun write(name: String, str: String, level: LogLevel) {
        if(level.isEnabledFor(logLevel).not()){
            return
        }

        if(colorize.not()){
            println(formatter.format(str, level))
        }else{
            getColor(level)?.let {
                println(Color.foreground(formatter.format(str, level), it))
            } ?: run {
                println(str)
            }
        }

    }
}