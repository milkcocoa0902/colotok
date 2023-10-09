package com.milkcocoa.info.clk.core.provider.details

import com.milkcocoa.info.clk.core.LogLevel
import com.milkcocoa.info.clk.util.color.AnsiColor

interface ProviderColorConfig {

    var traceLevelColor: AnsiColor
    var debugLevelColor: AnsiColor
    var infoLevelColor: AnsiColor
    var warnLevelColor: AnsiColor
    var errorLevelColor: AnsiColor

    fun getColorForLevel(level: LogLevel): AnsiColor?{
        return when(level){
            LogLevel.TRACE -> traceLevelColor
            LogLevel.DEBUG -> debugLevelColor
            LogLevel.INFO -> infoLevelColor
            LogLevel.WARN -> warnLevelColor
            LogLevel.ERROR -> errorLevelColor
            LogLevel.OFF -> null
        }
    }
}