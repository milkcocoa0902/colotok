package com.milkcocoa.info.clk.core.builtin

import com.milkcocoa.info.clk.core.details.Provider

class ConsoleProvider: Provider {
    override fun write(str: String) {
        println(str)
    }
}