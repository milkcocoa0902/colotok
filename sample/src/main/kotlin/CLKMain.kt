package com.milkcocoa.info.colotok

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.logger.ColotokLoggerContext
import com.milkcocoa.info.colotok.core.logger.infoAsync
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

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

    runBlocking {
        logger.infoAsync("test0001")
        logger.infoAsync("test002")
        logger.infoAsync("test003")
        logger.infoAsync("test004")

        delay(1000.milliseconds)
    }
}