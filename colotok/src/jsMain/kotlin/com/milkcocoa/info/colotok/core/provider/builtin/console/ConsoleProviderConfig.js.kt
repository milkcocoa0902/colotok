package com.milkcocoa.info.colotok.core.provider.builtin.console

import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.ProviderConfig

actual class ConsoleProviderConfig actual constructor() : ProviderConfig {
    actual override var level: Level = LogLevel.DEBUG
    actual override var formatter: Formatter = DetailTextFormatter
}