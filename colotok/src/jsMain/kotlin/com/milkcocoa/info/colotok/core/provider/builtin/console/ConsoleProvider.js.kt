package com.milkcocoa.info.colotok.core.provider.builtin.console

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.serialization.KSerializer

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

    actual override fun write(
        name: String,
        msg: String,
        level: Level,
        attr: Map<String, String>
    ) {
        if (level.isEnabledFor(logLevel).not()) {
            return
        }

        when (level) {
            LogLevel.TRACE -> console.log(formatter.format(msg, level, attr))
            LogLevel.DEBUG -> console.log(formatter.format(msg, level, attr))
            LogLevel.INFO -> console.info(formatter.format(msg, level, attr))
            LogLevel.WARN -> console.warn(formatter.format(msg, level, attr))
            LogLevel.ERROR -> console.error(formatter.format(msg, level, attr))
        }
    }

    actual override fun <T : LogStructure> write(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attr: Map<String, String>
    ) {
        if (level.isEnabledFor(logLevel).not()) {
            return
        }

        when (level) {
            LogLevel.TRACE -> console.log(formatter.format(msg, serializer, level, attr))
            LogLevel.DEBUG -> console.log(formatter.format(msg, serializer, level, attr))
            LogLevel.INFO -> console.info(formatter.format(msg, serializer, level, attr))
            LogLevel.WARN -> console.warn(formatter.format(msg, serializer, level, attr))
            LogLevel.ERROR -> console.error(formatter.format(msg, serializer, level, attr))
        }
    }
}