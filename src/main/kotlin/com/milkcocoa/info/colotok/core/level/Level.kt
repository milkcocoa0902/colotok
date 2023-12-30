package com.milkcocoa.info.colotok.core.level

abstract class Level(val levelInt: Int, val name: String) {
    final fun toInt() = levelInt
    final override fun toString() = name

    fun isEnabledFor(other: Level): Boolean = this.levelInt >= other.levelInt
}

object LogLevel{
    object TRACE: Level(LevelInt.TRACE_INT, "TRACE")
    object DEBUG: Level(LevelInt.DEBUG_INT, "DEBUG")
    object INFO: Level(LevelInt.INFO_INT, "INFO")
    object WARN: Level(LevelInt.WARN_INT, "WARN")
    object ERROR: Level(LevelInt.ERROR_INT, "ERROR")
    object OFF: Level(LevelInt.OFF_INT, "OFF")
}


fun Level.isEnabledForTrace() = isEnabledFor(LogLevel.TRACE)
fun Level.isEnabledForDebug() = isEnabledFor(LogLevel.DEBUG)
fun Level.isEnabledForInfo() = isEnabledFor(LogLevel.INFO)
fun Level.isEnabledForWarn() = isEnabledFor(LogLevel.WARN)
fun Level.isEnabledForError() = isEnabledFor(LogLevel.ERROR)