package com.milkcocoa.info.colotok.core.provider.builtin.console

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.util.color.AnsiColor
import com.milkcocoa.info.colotok.util.color.Color
import kotlinx.serialization.KSerializer

public actual class ConsoleProvider actual constructor(config: ConsoleProviderConfig) : Provider {
    constructor(config: ConsoleProviderConfig.() -> Unit) : this(ConsoleProviderConfig().apply(config))

    /**
     * initialize provider with default configuration
     */
    constructor() : this(ConsoleProviderConfig())

    actual val logLevel: Level = config.level
    actual val formatter: Formatter = config.formatter
    val colorize: Boolean = config.colorize
    val getColor: ((Level) -> AnsiColor?) = {
        config.getColorForLevel(level = it)
    }

    actual override fun write(
        name: String,
        msg: String,
        level: Level,
        attr: Map<String, String>
    ) {
        if (level.isEnabledFor(logLevel).not()) {
            return
        }

        runCatching {
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
        runCatching {
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