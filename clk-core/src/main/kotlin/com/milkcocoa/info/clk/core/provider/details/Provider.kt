package com.milkcocoa.info.clk.core.provider.details

import com.milkcocoa.info.clk.core.LogLevel
import com.milkcocoa.info.clk.core.formatter.builtin.DetailFormatter
import com.milkcocoa.info.clk.core.formatter.details.Formatter
import com.milkcocoa.info.clk.util.color.AnsiColor

interface Provider {
    val colorize: Boolean
        get() = true

    val formatter: Formatter get() = DetailFormatter

    val colors: Map<LogLevel, AnsiColor>
        get() = mapOf(
            LogLevel.TRACE to AnsiColor.WHITE,
            LogLevel.DEBUG to AnsiColor.BLUE,
            LogLevel.INFO to AnsiColor.GREEN,
            LogLevel.WARN to AnsiColor.YELLOW,
            LogLevel.ERROR to AnsiColor.RED
        )

    fun write(name: String, str: String, level: LogLevel)
}