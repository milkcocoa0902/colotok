package com.milkcocoa.info.colotok.core.provider.builtin.console

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.Provider

@OptIn(ExperimentalJsExport::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
public actual class ConsoleProvider actual constructor(config: ConsoleProviderConfig) : Provider {
    constructor(config: ConsoleProviderConfig.() -> Unit) : this(ConsoleProviderConfig().apply(config))

    /**
     * initialize provider with default configuration
     */
    constructor() : this(ConsoleProviderConfig())

    actual val logLevel: Level = config.level
    actual val formatter: Formatter = config.formatter


    actual override fun write(record: LogRecord) {
        if(record.level.isEnabledFor(logLevel).not()) return
        runCatching {
            when(record.level){
                LogLevel.TRACE -> console.log(record.format(formatter))
                LogLevel.DEBUG -> console.log(record.format(formatter))
                LogLevel.INFO -> console.info(record.format(formatter))
                LogLevel.WARN -> console.warn(record.format(formatter))
                LogLevel.ERROR -> console.error(record.format(formatter))
            }
        }
    }
}