package com.milkcocoa.info.colotok

import com.milkcocoa.info.colotok.core.formatter.builtin.structure.SimpleStructureFormatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.logger.LoggerFactory
import com.milkcocoa.info.colotok.core.provider.builtin.ConsoleProvider
import com.milkcocoa.info.colotok.core.provider.builtin.FileProvider
import com.milkcocoa.info.colotok.core.provider.builtin.StreamProvider
import kotlinx.serialization.Serializable

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

fun main() {
    val fileProvider: FileProvider
    val streamProvider: StreamProvider
    val logger =
        LoggerFactory()
            .addProvider(
                ConsoleProvider {
                    formatter = SimpleStructureFormatter
                }
            )
            .withAttrs(
                mapOf(
                    "def attr" to "attributes"
                )
            )
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

    logger.error(
        Credential(
            username = "user_name",
            password = "this field is masked",
            raw_password = "this field is not masked"
        ),
        Credential.serializer()
    )
}