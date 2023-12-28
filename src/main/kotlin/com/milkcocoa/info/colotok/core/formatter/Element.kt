package com.milkcocoa.info.colotok.core.formatter

enum class Element(private val typeString: String) {
    /**
     * year, ignored when StructuredFormat
     */
    YEAR("%Y"),

    /**
     * month, ignored when StructuredFormat
     */
    MONTH("%M"),

    /**
     * day, ignored when StructuredFormat
     */
    DAY("%D"),

    /**
     * hour, ignored when StructuredFormat
     */
    HOUR("%h"),

    /**
     * minute, ignored when StructuredFormat
     */
    MINUTE("%m"),

    /**
     * second, ignored when StructuedFormat
     */
    SECOND("%s"),

    /**
     * date(yyyy-MM-dd)
     */
    DATE("%d"),

    /**
     * time(HH:mm:ss)
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

    ATTR("%a");

    override fun toString(): String {
        return typeString
    }
}