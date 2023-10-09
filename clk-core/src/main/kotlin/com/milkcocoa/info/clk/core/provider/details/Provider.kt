package com.milkcocoa.info.clk.core.provider.details

import com.milkcocoa.info.clk.core.LogLevel
import com.milkcocoa.info.clk.core.formatter.builtin.DetailFormatter
import com.milkcocoa.info.clk.core.formatter.details.Formatter
import com.milkcocoa.info.clk.util.color.AnsiColor

interface Provider {
    fun write(name: String, str: String, level: LogLevel)
}

