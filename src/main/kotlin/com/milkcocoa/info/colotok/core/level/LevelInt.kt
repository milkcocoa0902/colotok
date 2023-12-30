package com.milkcocoa.info.colotok.core.level

object LevelInt {

    const val TRACE_INT: Int = 0
    const val DEBUG_INT: Int = 10
    const val INFO_INT: Int = 20
    const val WARN_INT: Int = 30
    const val ERROR_INT: Int = 40
    const val OFF_INT: Int = 50

    fun intToLevel(levelInt: Int): Level {
        return when (levelInt) {
            TRACE_INT -> LogLevel.TRACE
            DEBUG_INT -> LogLevel.DEBUG
            INFO_INT -> LogLevel.INFO
            WARN_INT -> LogLevel.WARN
            ERROR_INT -> LogLevel.ERROR
            OFF_INT -> LogLevel.OFF
            else -> throw IllegalArgumentException("Level integer [$levelInt] not recognized.")
        }
    }
}