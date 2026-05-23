package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.builtin.file.FileProvider
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class ShutdownEffectTest {
    @Test
    fun testLogLossWithoutShutdown() {
        val testLogFile = File("loss_test.log")
        if(testLogFile.exists()) testLogFile.delete()
        
        val logger = ColotokLogger("loss-test") {
            providers = listOf(FileProvider(testLogFile.toOkioPath()) {
                level = LogLevel.DEBUG
            })
        }
        
        logger.info("this log might be lost")
        
        // 終了を待たずにファイルを確認
        val content = if (testLogFile.exists()) testLogFile.readText() else ""
        println("Log content without shutdown: '$content'")
        // Channel経由なので、この時点ではまだ書き込まれていない可能性が高い
    }

    @Test
    fun testLogPreservationWithShutdown() = runBlocking {
        val testLogFile = File("preserved_test.log")
        if(testLogFile.exists()) testLogFile.delete()

        val logger = ColotokLogger("preserved-test") {
            providers = listOf(FileProvider(testLogFile.toOkioPath()) {
                level = LogLevel.DEBUG
            })
        }

        logger.info("this log must be preserved")
        logger.shutdown()

        Assertions.assertTrue(testLogFile.exists())
        val content = testLogFile.readText()
        println("Log content with shutdown: '$content'")
        Assertions.assertTrue(content.contains("this log must be preserved"))
    }
}
