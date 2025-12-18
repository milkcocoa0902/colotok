package com.milkcocoa.info.colotok

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.logger.ColotokLoggerContext
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProvider
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
    val logger = ColotokLoggerContext()
        .addProvider(ConsoleProvider())
        .getLogger()

    logger.info("test001")
    logger.info("test002")
    logger.info("test003")
    logger.info("test004")
}