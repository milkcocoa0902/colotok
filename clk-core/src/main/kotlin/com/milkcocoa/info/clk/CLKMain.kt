package com.milkcocoa.info.clk

import com.milkcocoa.info.clk.core.LogLevel
import com.milkcocoa.info.clk.core.LoggerFactory
import com.milkcocoa.info.clk.core.provider.builtin.ConsoleProvider
import com.milkcocoa.info.clk.core.provider.builtin.FileProvider
class CLKMain

fun main() {
    val logger = LoggerFactory()
        .addProvider(ConsoleProvider())
        .addProvider(FileProvider("test.log"))
        .setLogLevel(LogLevel.TRACE)
        .getLogger()

    logger.trace("TRACE LEVEL LGO")
    logger.debug("DEBUG LEVEL LOG")
    logger.info("INFO LEVEL LOG")
    logger.warn("WARN LEVEL LOG")
    logger.error("ERROR LEVEL LOG")
}