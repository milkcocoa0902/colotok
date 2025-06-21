package com.milkcocoa.info.colotok

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.formatter.builtin.text.SimpleTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.formatter.details.StructuredFormatter
import com.milkcocoa.info.colotok.core.logger.ColotokLogger
import com.milkcocoa.info.colotok.core.logger.ColotokLoggerFactory
import com.milkcocoa.info.colotok.core.logger.MDC
import com.milkcocoa.info.colotok.core.logger.withMdcScope
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProvider
import com.milkcocoa.info.colotok.core.provider.builtin.file.FileProvider
import com.milkcocoa.info.colotok.core.provider.builtin.stream.StreamProvider
import com.milkcocoa.info.colotok.core.provider.rotation.SizeBaseRotation
import com.milkcocoa.info.colotok.util.unit.Size.KiB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okio.Path.Companion.toOkioPath
import org.slf4j.LoggerFactory
import java.io.File

class CLKMain

@Serializable
class LogDetail(val scope: String, val message: String) : LogStructure

@Serializable
class Log(val name: String, val logDetail: LogDetail) : LogStructure

@Serializable
class Credential(
    val username: String,
    val password: String,
    val raw_password: String
) : LogStructure

suspend fun main() {
//    MDC.put("TEST", "test")

//    val fileProvider: FileProvider =
//        FileProvider(File("test.log").toOkioPath()) {
//            this.formatter = SimpleTextFormatter
//            this.enableBuffer = false
//            this.rotation = SizeBaseRotation(4L.KiB())
//        }
//    val streamProvider: StreamProvider
//        ColotokLoggerFactory()
//            .addProvider(
//                ConsoleProvider {
//                    colorize = false
//                    formatter =
//                        object : StructuredFormatter(
//                            listOf(
//                                Element.MESSAGE,
//                                Element.LEVEL,
//                                Element.DATETIME,
//                                Element.THREAD,
//                                Element.ATTR,
//                                Element.CUSTOM("TEST"),
//                                Element.CUSTOM("jjj")
//                            ),
//                            listOf(
//                                "password",
//                                "secret"
//                            )
//                        ) {}
//                }
//            ).addProvider(fileProvider)
//            .withAttrs(
//                mapOf(
//                    "def attr" to "attributes"
//                )
//            )
//            .getLogger().let {
//                ColotokLogger.setDefault(it)
//            }

    val logger = LoggerFactory.getLogger("test")

    logger.trace("TRACE LEVEL LOG")
    logger.debug("DEBUG LEVEL LOG")

    Thread {
        MDC.put("TEST", "testtttttttttttt")
        logger.info("INFO LEVEL LOG")
        logger.warn("WARN LEVEL LOG")
        MDC.clear()
    }.start()

    Thread {
        MDC.put("TEST", "testtttttttttttt")
        logger.info("INFO LEVEL LOG")
        logger.warn("WARN LEVEL LOG")
        MDC.clear()
    }.start()

    logger.info("INFO LEVEL LOG")
    logger.warn("WARN LEVEL LOG")
    logger.error("ERROR LEVEL LOG")


    withMdcScope(Dispatchers.Default) {
        logger.info("INFO LEVEL LOG???????")
        withContext(Dispatchers.Default) {
            MDC.put("TEST", "testtttttttttttt")
            logger.info("INFO LEVEL LOG")
        }
    }
    logger.error("ERROR LEVEL LOG")

//    MDC.clear()
//    fileProvider.flush()
}