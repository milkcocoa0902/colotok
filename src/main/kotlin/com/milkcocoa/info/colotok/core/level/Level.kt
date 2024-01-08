package com.milkcocoa.info.colotok.core.level

import com.milkcocoa.info.colotok.util.color.AnsiColor

abstract class Level(val levelInt: Int, val name: String, val color: AnsiColor) {
    final fun toInt() = levelInt

    final override fun toString() = name

    fun isEnabledFor(other: Level): Boolean = this.levelInt >= other.levelInt
}

object LogLevel {
    object TRACE : Level(
        LevelInt.TRACE_INT,
        "TRACE",
        AnsiColor.WHITE
    )

    object DEBUG : Level(
        LevelInt.DEBUG_INT,
        "DEBUG",
        AnsiColor.BLUE
    )

    object INFO : Level(
        LevelInt.INFO_INT,
        "INFO",
        AnsiColor.GREEN
    )

    object WARN : Level(
        LevelInt.WARN_INT,
        "WARN",
        AnsiColor.YELLOW
    )

    object ERROR : Level(
        LevelInt.ERROR_INT,
        "ERROR",
        AnsiColor.RED
    )

    object OFF : Level(
        LevelInt.OFF_INT,
        "OFF",
        AnsiColor.BLACK
    )
}

fun Level.isEnabledForTrace() = isEnabledFor(LogLevel.TRACE)

fun Level.isEnabledForDebug() = isEnabledFor(LogLevel.DEBUG)

fun Level.isEnabledForInfo() = isEnabledFor(LogLevel.INFO)

fun Level.isEnabledForWarn() = isEnabledFor(LogLevel.WARN)

fun Level.isEnabledForError() = isEnabledFor(LogLevel.ERROR)