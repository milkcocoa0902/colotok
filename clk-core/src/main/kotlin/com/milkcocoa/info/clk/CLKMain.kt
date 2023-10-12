package com.milkcocoa.info.clk

import com.milkcocoa.info.clk.core.logger.LogLevel
import com.milkcocoa.info.clk.core.logger.LoggerFactory
import com.milkcocoa.info.clk.core.formatter.builtin.SimpleFormatter
import com.milkcocoa.info.clk.core.provider.builtin.ConsoleProvider
import com.milkcocoa.info.clk.core.provider.builtin.FileProvider
import com.milkcocoa.info.clk.core.provider.rotation.SizeBaseRotation
import java.nio.file.Path

class CLKMain

fun main() {
val fileProvider: FileProvider
val logger = LoggerFactory()
    .addProvider(ConsoleProvider{
        formatter = SimpleFormatter
    })
    .addProvider(FileProvider(Path.of("./test.log")){
        logLevel = LogLevel.TRACE
        formatter = SimpleFormatter
        enableBuffer = true
        bufferSize = 2048
        rotation = SizeBaseRotation(size = 4096)
    }.apply {
        fileProvider = this
    })
    .getLogger()

    logger.trace("TRACE LEVEL LOG")
    logger.debug("DEBUG LEVEL LOG")
    logger.info("INFO LEVEL LOG")
    logger.warn("WARN LEVEL LOG")
    logger.error("ERROR LEVEL LOG")

    logger.atInfo {
        print("afadfadf")
        print("afadfaf")
        print("afe")
        val a = 1
        print("$a")
    }
    fileProvider.flush()
}