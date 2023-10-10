package com.milkcocoa.info.clk.core.logger

enum class LogLevel(private val levelInt: Int, private val levelStr: String) {
    TRACE(Levels.TRACE_INT, "TRACE"),
    DEBUG(Levels.DEBUG_INT, "DEBUG"),
    INFO(Levels.INFO_INT, "INFO"),
    WARN(Levels.WARN_INT, "WARN"),
    ERROR(Levels.ERROR_INT, "ERROR"),
    OFF(Levels.OFF_INT, "OFF"),
    ;

    public fun toInt(): Int {
        return levelInt
    }

    public fun isEnabledTrace(): Boolean = isEnabledFor(TRACE)
    public fun isEnabledDebug(): Boolean = isEnabledFor(DEBUG)
    public fun isEnabledInfo(): Boolean = isEnabledFor(INFO)
    public fun isEnabledWarn(): Boolean = isEnabledFor(WARN)
    public fun isEnabledError(): Boolean = isEnabledFor(ERROR)
    public fun isEnabledFor(other: LogLevel): Boolean = this.levelInt >= other.levelInt

    /** Returns the string representation of this Level. */
    override fun toString(): String {
        return levelStr
    }
}

public object Levels {

    public const val TRACE_INT: Int = 0
    public const val DEBUG_INT: Int = 10
    public const val INFO_INT: Int = 20
    public const val WARN_INT: Int = 30
    public const val ERROR_INT: Int = 40
    public const val OFF_INT: Int = 50

    public fun intToLevel(levelInt: Int): LogLevel {
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