package com.milkcocoa.info.colotok.core.provider.builtin.console

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.provider.details.ProviderConfig

/**
 * builtin provider which used to write the log into console.
 */

expect class ConsoleProviderConfig() : ProviderConfig {
    override var level: Level // = LogLevel.DEBUG
    override var formatter: Formatter // = DetailTextFormatter
}