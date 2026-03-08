package com.milkcocoa.info.colotok.core.provider.builtin.console

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.util.color.AnsiColor
import com.milkcocoa.info.colotok.util.color.Color

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
public actual class ConsoleProvider actual constructor(config: ConsoleProviderConfig) : Provider(config) {
    constructor(config: ConsoleProviderConfig.() -> Unit) : this(ConsoleProviderConfig().apply(config))

    /**
     * initialize provider with default configuration
     */
    constructor() : this(ConsoleProviderConfig())

    private val colorize: Boolean = config.colorize
    private val getColor: ((Level) -> AnsiColor?) = {
        config.getColorForLevel(level = it)
    }


    actual override suspend fun onMessage(record: LogRecord) {
        runCatching {
            val color = when{
                colorize.not() -> null
                else -> getColor(record.level)
            }
            if(color != null){
                println(Color.foreground(record.format(config.formatter), color))
            }else{
                println(record.format(config.formatter))
            }
        }
    }
}