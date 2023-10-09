package com.milkcocoa.info.clk.core.provider.builtin

import com.milkcocoa.info.clk.core.LogLevel
import com.milkcocoa.info.clk.core.provider.details.Provider
import com.milkcocoa.info.clk.util.color.Color

class ConsoleProvider : Provider {
    override fun write(name: String, str: String, level: LogLevel) {
        colors.get(level)?.let {
            println(Color.foreground(formatter.format(str, level), it))
        } ?: run {
            println(str)
        }
    }
}