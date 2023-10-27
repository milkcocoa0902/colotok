package com.milkcocoa.info.clk.core.provider.details

import com.milkcocoa.info.clk.core.logger.LogLevel

interface Provider {
    fun write(name: String, str: String, level: LogLevel)
}

