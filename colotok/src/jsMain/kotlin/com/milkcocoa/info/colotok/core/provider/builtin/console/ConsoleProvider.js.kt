package com.milkcocoa.info.colotok.core.provider.builtin.console

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.Provider

@OptIn(ExperimentalJsExport::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
public actual class ConsoleProvider actual constructor(config: ConsoleProviderConfig) : Provider(config) {
    constructor(config: ConsoleProviderConfig.() -> Unit) : this(ConsoleProviderConfig().apply(config))

    /**
     * initialize provider with default configuration
     */
    constructor() : this(ConsoleProviderConfig())


    actual override suspend fun onMessage(record: LogRecord) {
        runCatching {
            when(record.level){
                LogLevel.TRACE -> console.log(record.format(config.formatter))
                LogLevel.DEBUG -> console.log(record.format(config.formatter))
                LogLevel.INFO -> console.info(record.format(config.formatter))
                LogLevel.WARN -> console.warn(record.format(config.formatter))
                LogLevel.ERROR -> console.error(record.format(config.formatter))
                else -> console.log(record.format(config.formatter))
            }
        }
    }
}