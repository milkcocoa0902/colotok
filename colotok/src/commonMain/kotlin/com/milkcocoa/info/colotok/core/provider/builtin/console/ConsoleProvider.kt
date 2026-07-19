package com.milkcocoa.info.colotok.core.provider.builtin.console

import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.Provider

public expect class ConsoleProvider(config: ConsoleProviderConfig) : Provider {
    override suspend fun onMessage(record: LogRecord): Unit
}