package com.milkcocoa.info.clk

import com.milkcocoa.info.clk.core.LogLevel
import com.milkcocoa.info.clk.core.LoggerFactory
import com.milkcocoa.info.clk.core.provider.builtin.ConsoleProvider
import com.milkcocoa.info.clk.core.provider.builtin.FileProvider
import com.milkcocoa.info.clk.core.provider.rotation.SizeBaseRotation

class CLKMain

fun main() {
    val fileProvider: FileProvider
    val logger = LoggerFactory()
        .addProvider(ConsoleProvider())
        .addProvider(FileProvider("./test.log"){
            enableBuffer = true
            bufferSize = 2048
            rotation = SizeBaseRotation(size = 512)
        }.apply {
            fileProvider = this
        })
        .setLogLevel(LogLevel.TRACE)
        .getLogger()

    logger.trace("TRACE LEVEL LOG")
    logger.debug("DEBUG LEVEL LOG")
    logger.info("INFO LEVEL LOG")
    logger.warn("WARN LEVEL LOG")
    logger.error("ERROR LEVEL LOG")

    fileProvider.flush()
}