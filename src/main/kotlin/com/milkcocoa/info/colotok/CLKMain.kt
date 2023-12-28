package com.milkcocoa.info.colotok

import com.milkcocoa.info.colotok.core.formatter.builtin.structure.DetailStructureFormatter
import com.milkcocoa.info.colotok.core.formatter.builtin.structure.SimpleStructureFormatter
import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.logger.LogLevel
import com.milkcocoa.info.colotok.core.logger.LoggerFactory
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.provider.builtin.ConsoleProvider
import com.milkcocoa.info.colotok.core.provider.builtin.FileProvider
import com.milkcocoa.info.colotok.core.provider.rotation.SizeBaseRotation
import kotlinx.serialization.Serializable
import java.nio.file.Path

class CLKMain

@Serializable
class LogDetail(val scope: String, val message: String): LogStructure

@Serializable
class Log(val name: String, val logDetail: LogDetail): LogStructure

fun main() {
val fileProvider: FileProvider
val logger = LoggerFactory()
    .addProvider(ConsoleProvider{
        formatter = DetailStructureFormatter
    })
    .addProvider(ConsoleProvider{
        formatter = DetailTextFormatter
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

    logger.info(
        Log(
            name = "illegal state",
            LogDetail(
                "args",
                "argument must be greater than zero"
            )
        ),
        Log.serializer()
    )



    logger.trace("TRACE LEVEL LOG")
    logger.debug("DEBUG LEVEL LOG")
    logger.info("INFO LEVEL LOG")
    logger.warn("WARN LEVEL LOG")
    logger.error("ERROR LEVEL LOG")

    logger.atInfo {
        print("in this block")
        print("all of logs are printed out with INFO level")

        print(
            Log(
                name = "illegal state",
                LogDetail(
                    "args",
                    "argument must be greater than zero"
                )
            ),
            Log.serializer()
        )
    }
    fileProvider.flush()
}