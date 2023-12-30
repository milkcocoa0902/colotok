package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.util.color.AnsiColor

/**
 * provider's color config for each level
 */
interface ProviderColorConfig {

    /**
     * do colorize. if true, provider can print message with ANSI color code.
     */
    var colorize: Boolean

    /**
     * the color which used for trace level log
     */
    var traceLevelColor: AnsiColor

    /**
     * the color which used for debug level log
     */
    var debugLevelColor: AnsiColor

    /**
     * the color which used for info level log
     */
    var infoLevelColor: AnsiColor

    /**
     * the color which  used for warn level log
     */
    var warnLevelColor: AnsiColor

    /**
     * the color which used for error level log
     */
    var errorLevelColor: AnsiColor


    /**
     * returns the color for level.
     * @param level[Level] target log level
     * @return [AnsiColor] color
     */
    fun getColorForLevel(level: Level): AnsiColor?{
        return when(level){
            LogLevel.TRACE -> traceLevelColor
            LogLevel.DEBUG -> debugLevelColor
            LogLevel.INFO -> infoLevelColor
            LogLevel.WARN -> warnLevelColor
            LogLevel.ERROR -> errorLevelColor
            LogLevel.OFF -> null
            else -> level.color
        }
    }
}