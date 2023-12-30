package com.milkcocoa.info.colotok

import com.milkcocoa.info.colotok.core.formatter.builtin.structure.DetailStructureFormatter
import com.milkcocoa.info.colotok.core.formatter.builtin.structure.SimpleStructureFormatter
import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.logger.LoggerFactory
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.builtin.ConsoleProvider
import com.milkcocoa.info.colotok.core.provider.builtin.FileProvider
import com.milkcocoa.info.colotok.core.provider.builtin.StreamProvider
import com.milkcocoa.info.colotok.core.provider.rotation.SizeBaseRotation
import com.milkcocoa.info.colotok.util.unit.Size.KiB
import kotlinx.serialization.Serializable
import java.io.FileOutputStream
import java.nio.file.Path

class CLKMain

@Serializable
class LogDetail(val scope: String, val message: String): LogStructure

@Serializable
class Log(val name: String, val logDetail: LogDetail): LogStructure

fun main() {
val fileProvider: FileProvider
val streamProvider: StreamProvider
val logger = LoggerFactory()
    .addProvider(ConsoleProvider{
        formatter = DetailStructureFormatter
    })
    .addProvider(StreamProvider{
        enableBuffer = true
        bufferSize = 64.KiB()
        formatter = SimpleStructureFormatter
        outputStreamBuilder = {
            FileOutputStream("./stream-log", true)
        }
    }.apply { streamProvider = this })
    .addProvider(FileProvider(Path.of("./test.log")){
        level = LogLevel.TRACE
        formatter = DetailTextFormatter
        enableBuffer = true
        bufferSize = 2048
        rotation = SizeBaseRotation(size = 8192)
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
        Log.serializer(),
        mapOf("a" to "afeafseaf")
    )



    logger.trace("TRACE LEVEL LOG", mapOf("a" to "BBBBB"))
    logger.debug("DEBUG LEVEL LOG", mapOf("a" to "BBBBB"))
    logger.info("INFO LEVEL LOG",  mapOf("a" to "BBBBB"))
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
                    "argument must be greater than zeroooooooooooooooooo"
                )
            ),
            Log.serializer(),
            mapOf("a" to "afsegfewgrewg")
        )
    }
    fileProvider.flush()
    streamProvider.flush()
}