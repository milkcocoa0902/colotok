package com.milkcocoa.info.colotok.core.logger

/**
 * log level
 *
 * @param levelInt[Int] level value
 * @param levelStr[String] level name
 */
enum class LogLevel(private val levelInt: Int, private val levelStr: String) {

    /**
     * trace level. which means verbose output
     */
    TRACE(Levels.TRACE_INT, "TRACE"),

    /**
     * debug level.
     */
    DEBUG(Levels.DEBUG_INT, "DEBUG"),

    /**
     * info level
     */
    INFO(Levels.INFO_INT, "INFO"),

    /**
     * warn level
     */
    WARN(Levels.WARN_INT, "WARN"),

    /**
     * error level
     */
    ERROR(Levels.ERROR_INT, "ERROR"),

    /**
     * off. which means silent
     */
    OFF(Levels.OFF_INT, "OFF"),
    ;

    /**
     * @return [Int] [levelInt]
     */
    public fun toInt(): Int {
        return levelInt
    }

    /**
     * check current level is higher than [LogLevel.TRACE]
     * @return [Boolean] true if current level is higher than [LogLevel.TRACE]
     */
    public fun isEnabledTrace(): Boolean = isEnabledFor(TRACE)

    /**
     * check current level is higher than [LogLevel.DEBUG]
     * @return [Boolean] true if current level is higher than [LogLevel.DEBUG]
     */
    public fun isEnabledDebug(): Boolean = isEnabledFor(DEBUG)
    /**
     * check current level is higher than [LogLevel.INFO]
     * @return [Boolean] true if current level is higher than [LogLevel.INFO]
     */
    public fun isEnabledInfo(): Boolean = isEnabledFor(INFO)

    /**
     * check current level is higher than [LogLevel.WARN]
     * @return [Boolean] true if current level is higher than [LogLevel.WARN]
     */
    public fun isEnabledWarn(): Boolean = isEnabledFor(WARN)

    /**
     * check current level is higher than [LogLevel.ERROR]
     * @return [Boolean] true if current level is higher than [LogLevel.ERROR]
     */
    public fun isEnabledError(): Boolean = isEnabledFor(ERROR)

    /**
     * check current level is higher for [other] level. true if available false otherwise.
     */
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