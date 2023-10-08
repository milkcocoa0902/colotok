package com.milkcocoa.info.clk.core.details

import com.milkcocoa.info.clk.util.color.AnsiColor

interface Provider {
    val colorize: Boolean
        get() = true
    fun write(str: String)

}