package com.milkcocoa.info.clk.core.formatter

import java.util.Formattable
import java.util.Formatter

enum class Element(private val typeString: String) {
    YEAR("%Y"),
    MONTH("%M"),
    DAY("%D"),
    HOUR("%h"),
    MINUTE("%m"),
    SECOND("%s"),

    LEVEL("%L"),
    MESSAGE("%l"),
    THREAD("%t");

    override fun toString(): String {
        return typeString
    }
}