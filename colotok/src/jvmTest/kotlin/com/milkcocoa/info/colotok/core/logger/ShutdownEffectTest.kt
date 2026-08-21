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

            val logger =
                ColotokLogger("preserved-test") {
                    providers =
                        listOf(
                            FileProvider(testLogFile.toOkioPath()) {
                                level = LogLevel.DEBUG
                            }
                        )
                }

            try {
                logger.info("this log must be preserved")
                logger.shutdown()

                Assertions.assertTrue(testLogFile.exists())
                Assertions.assertTrue(testLogFile.readText().contains("this log must be preserved"))
            } finally {
                runCatching { logger.forceShutdown() }
                testLogFile.delete()
            }
        }
}