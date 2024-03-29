package com.milkcocoa.info.colotok.core.provider.builtin

import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.core.provider.details.ProviderColorConfig
import com.milkcocoa.info.colotok.core.provider.details.ProviderConfig
import com.milkcocoa.info.colotok.util.color.AnsiColor
import com.milkcocoa.info.colotok.util.color.Color
import kotlinx.serialization.KSerializer

/**
 * builtin provider which used to write the log into console.
 */
class ConsoleProvider(config: ConsoleProviderConfig) : Provider {
    constructor(config: ConsoleProviderConfig.() -> Unit) : this(ConsoleProviderConfig().apply(config))

    /**
     * initialize provider with default configuration
     */
    constructor() : this(ConsoleProviderConfig())

    class ConsoleProviderConfig : ProviderConfig, ProviderColorConfig {
        override var level: Level = LogLevel.DEBUG
        override var formatter: Formatter = DetailTextFormatter
        override var colorize: Boolean = true

        override var traceLevelColor: AnsiColor = LogLevel.TRACE.color
        override var debugLevelColor: AnsiColor = LogLevel.DEBUG.color
        override var infoLevelColor: AnsiColor = LogLevel.INFO.color
        override var warnLevelColor: AnsiColor = LogLevel.WARN.color
        override var errorLevelColor: AnsiColor = LogLevel.ERROR.color
    }

    private val logLevel = config.level
    private val formatter = config.formatter
    private var colorize = config.colorize
    private val getColor: ((Level) -> AnsiColor?) = {
        config.getColorForLevel(it)
    }

    override fun write(
        name: String,
        msg: String,
        level: Level,
        attr: Map<String, String>
    ) {
        if (level.isEnabledFor(logLevel).not()) {
            return
        }

        kotlin.runCatching {
            if (colorize.not()) {
                println(formatter.format(msg, level, attr))
            } else {
                getColor(level)?.let {
                    println(Color.foreground(formatter.format(msg, level, attr), it))
                } ?: run {
                    println(formatter.format(msg, level, attr))
                }
            }
        }
    }

    override fun <T : LogStructure> write(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attr: Map<String, String>
    ) {
        if (level.isEnabledFor(logLevel).not()) {
            return
        }
        kotlin.runCatching {
            if (colorize.not()) {
                println(formatter.format(msg, serializer, level, attr))
            } else {
                getColor(level)?.let {
                    println(Color.foreground(formatter.format(msg, serializer, level, attr), it))
                } ?: run {
                    println(formatter.format(msg, serializer, level, attr))
                }
            }
        }
    }
}