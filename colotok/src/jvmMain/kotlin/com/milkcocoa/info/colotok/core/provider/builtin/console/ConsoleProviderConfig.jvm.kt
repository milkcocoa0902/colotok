package com.milkcocoa.info.colotok.core.provider.builtin.console

import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.ProviderColorConfig
import com.milkcocoa.info.colotok.core.provider.details.ProviderConfig
import com.milkcocoa.info.colotok.util.color.AnsiColor

actual class ConsoleProviderConfig actual constructor() : ProviderConfig, ProviderColorConfig {
    actual override var level: Level = LogLevel.DEBUG
    actual override var formatter: Formatter = DetailTextFormatter
    override var colorize: Boolean = true
    override var traceLevelColor: AnsiColor = LogLevel.TRACE.color
    override var debugLevelColor: AnsiColor = LogLevel.DEBUG.color
    override var infoLevelColor: AnsiColor = LogLevel.INFO.color
    override var warnLevelColor: AnsiColor = LogLevel.WARN.color
    override var errorLevelColor: AnsiColor = LogLevel.ERROR.color
}