package com.milkcocoa.info.colotok.core.provider.builtin.console

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.Provider

public expect class ConsoleProvider(config: ConsoleProviderConfig) : Provider {
    val logLevel: Level
    val formatter: Formatter

    override fun onMessage(record: LogRecord): Unit
}