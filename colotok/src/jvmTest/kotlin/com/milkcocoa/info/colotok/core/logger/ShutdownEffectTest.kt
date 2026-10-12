package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.builtin.file.FileProvider
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class ShutdownEffectTest {
    @Test
    fun shutdown_preserves_accepted_file_log() =
        runBlocking {
            val testLogFile = File("preserved_test.log")
            testLogFile.delete()

            val provider = FileProvider(testLogFile.toOkioPath()) { level = LogLevel.DEBUG }
            val context = ColotokLoggerContext().addProvider(provider)
            val logger = context.getLogger("preserved-test")

            try {
                logger.info("this log must be preserved")
                context.shutdown()

                Assertions.assertTrue(testLogFile.exists())
                Assertions.assertTrue(testLogFile.readText().contains("this log must be preserved"))
            } finally {
                context.forceShutdown()
                runCatching { provider.join() }
                testLogFile.delete()
            }
        }
}