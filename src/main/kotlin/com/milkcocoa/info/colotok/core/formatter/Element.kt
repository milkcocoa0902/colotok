package com.milkcocoa.info.colotok.core.formatter

enum class Element(private val typeString: String) {
    /**
     * date time(yyyy-MM-dd'T'HH:mm:ss.SSS+offset)
     */
    DATE_TIME("%D"),

    /**
     * date(yyyy-MM-dd). <br />
     *
     * when structured logging ,
     * ignored if [DATE_TIME] is also specified,
     * and if used with [TIME], works like [DATE_TIME]
     */
    DATE("%d"),

    /**
     * time(HH:mm:ss)
     *
     * when structured logging ,
     * ignored if [DATE_TIME] is also specified,
     * and if used with [DATE], works like [DATE_TIME]
     */
    TIME("%T"),

    /**
     * log level
     */
    LEVEL("%L"),

    /**
     * log message
     */
    MESSAGE("%l"),

    /**
     * logging thread
     */
    THREAD("%t"),

    /**
     * logger name
     */
    NAME("%n"),

    /**
     * additional attrs
     */
    ATTR("%a");

    override fun toString(): String {
        return typeString
    }
}