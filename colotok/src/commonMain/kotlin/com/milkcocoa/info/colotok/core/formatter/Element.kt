package com.milkcocoa.info.colotok.core.formatter

sealed class Element(private val typeString: String) {
    /**
     * date time(yyyy-MM-dd'T'HH:mm:ss.SSS+offset)
     */
    object DATETIME : Element("%D")

    /**
     * date(yyyy-MM-dd). <br />
     *
     * when structured logging ,
     * ignored if [DATETIME] is also specified,
     * and if used with [TIME], works like [DATETIME]
     */
    object DATE : Element("%d")

    /**
     * time(HH:mm:ss)
     *
     * when structured logging ,
     * ignored if [DATETIME] is also specified,
     * and if used with [DATE], works like [DATETIME]
     */
    object TIME : Element("%T")

    /**
     * log level
     */
    object LEVEL : Element("%L")

    /**
     * log message
     */
    object MESSAGE : Element("%l")

    /**
     * logging thread
     */
    object THREAD : Element("%t")

    /**
     * logger name
     */
    object NAME : Element("%n")

    /**
     * additional attrs
     */
    object ATTR : Element("%a")

    object CALLER : Element("%C")

    class CUSTOM(val raw: String) : Element("%$raw")

    override fun toString(): String {
        return typeString
    }
}