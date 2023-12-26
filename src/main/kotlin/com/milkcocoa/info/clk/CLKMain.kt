package com.milkcocoa.info.clk

import com.milkcocoa.info.clk.core.formatter.builtin.structure.DetailStructureFormatter
import com.milkcocoa.info.clk.core.formatter.builtin.structure.SimpleStructureFormatter
import com.milkcocoa.info.clk.core.logger.LogLevel
import com.milkcocoa.info.clk.core.logger.LoggerFactory
import com.milkcocoa.info.clk.core.formatter.details.LogStructure
import com.milkcocoa.info.clk.core.provider.builtin.ConsoleProvider
import com.milkcocoa.info.clk.core.provider.builtin.FileProvider
import com.milkcocoa.info.clk.core.provider.rotation.SizeBaseRotation
import kotlinx.serialization.Serializable
import java.nio.file.Path

class CLKMain

@Serializable
class A(val b: String, val ccc: String): LogStructure

@Serializable
class AAA(val ddd: String, val a: A): LogStructure

fun main() {
val fileProvider: FileProvider
val logger = LoggerFactory()
    .addProvider(ConsoleProvider{
        formatter = DetailStructureFormatter
    })
    .addProvider(FileProvider(Path.of("./test.log")){
        logLevel = LogLevel.TRACE
        formatter = SimpleStructureFormatter
        enableBuffer = true
        bufferSize = 2048
        rotation = SizeBaseRotation(size = 4096)
    }.apply {
        fileProvider = this
    })
    .getLogger()

    logger.info(AAA("afaffaefe", A("aaa", "afsdf")), AAA.serializer())

//    logger.trace("TRACE LEVEL LOG")
//    logger.debug("DEBUG LEVEL LOG")
//    logger.info("INFO LEVEL LOG")
//    logger.warn("WARN LEVEL LOG")
//    logger.error("ERROR LEVEL LOG")

//    logger.atInfo {
//        print("in this block")
//        print("all of logs are printed out with INFO level")
//    }
    fileProvider.flush()
}