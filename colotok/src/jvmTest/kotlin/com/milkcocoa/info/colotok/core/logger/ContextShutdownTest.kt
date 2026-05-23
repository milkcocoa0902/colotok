package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.builtin.file.FileProvider
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class ContextShutdownTest {
    @Test
    fun testContextShutdown() = runBlocking {
        println("Running testContextShutdown")
        val testLogFile1 = File("context_shutdown_1.log")
        val testLogFile2 = File("context_shutdown_2.log")
        testLogFile1.delete()
        testLogFile2.delete()

        val context = ColotokLoggerContext()
        context.addProvider(FileProvider(testLogFile1.toOkioPath()) {
            level = LogLevel.DEBUG
        })
        context.addProvider(FileProvider(testLogFile2.toOkioPath()) {
            level = LogLevel.DEBUG
        })

        val logger1 = context.getLogger("logger1")
        val logger2 = context.getLogger("logger2")

        logger1.info("log 1")
        logger2.info("log 2")

        context.shutdown()

        Assertions.assertTrue(testLogFile1.exists())
        Assertions.assertTrue(testLogFile2.exists())
        
        val content1 = testLogFile1.readText()
        val content2 = testLogFile2.readText()
        
        Assertions.assertTrue(content1.contains("log 1"))
        Assertions.assertTrue(content1.contains("log 2")) // 両方のloggerが同じproviderを持つため
        Assertions.assertTrue(content2.contains("log 1"))
        Assertions.assertTrue(content2.contains("log 2"))
        
        testLogFile1.delete()
        testLogFile2.delete()
    }
}
