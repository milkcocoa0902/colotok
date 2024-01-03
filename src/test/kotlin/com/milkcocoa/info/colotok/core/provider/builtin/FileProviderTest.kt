package com.milkcocoa.info.colotok.core.provider.builtin

import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.formatter.builtin.text.SimpleTextFormatter
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.util.unit.Size.KiB
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.attribute.FileAttribute
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.notExists
import kotlin.io.path.readLines

object FileProviderTest {
    val testLogFile = java.nio.file.Path.of("./junit-test-log.log")
    @BeforeEach
    fun before(){
        if(testLogFile.notExists()){
            testLogFile.createFile()
        }
    }

    @AfterEach
    fun after(){
        testLogFile.deleteIfExists()
        unmockkAll()
    }



    @Test
    fun fileProviderTest01(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56Z")

        val provider = FileProvider(testLogFile){
            enableBuffer = false
            level = LogLevel.DEBUG
            formatter = SimpleTextFormatter
        }

        provider.write(
            name = "default logger",
            msg = "message",
            level = LogLevel.INFO
        )


        Assertions.assertEquals(
            "2023-12-31 12:34:56  [INFO] - message",
            testLogFile.readLines(Charsets.UTF_8).getOrNull(0)
        )
    }


    @Test
    fun fileProviderTest02(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56Z")

        val provider = FileProvider(testLogFile){
            enableBuffer = true
            bufferSize = 4096.KiB()
            level = LogLevel.DEBUG
            formatter = SimpleTextFormatter
        }

        provider.write(
            name = "default logger",
            msg = "message",
            level = LogLevel.INFO
        )


        Assertions.assertEquals(
            "",
            testLogFile.readLines(Charsets.UTF_8).getOrNull(0) ?: ""
        )

        provider.flush()
        Assertions.assertEquals(
            "2023-12-31 12:34:56  [INFO] - message",
            testLogFile.readLines(Charsets.UTF_8).getOrNull(0)
        )
    }



    @Test
    fun fileProviderTest03(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56Z")

        val provider = FileProvider(testLogFile){
            enableBuffer = true
            bufferSize = 16
            level = LogLevel.DEBUG
            formatter = SimpleTextFormatter
        }

        provider.write(
            name = "default logger",
            msg = "message",
            level = LogLevel.INFO
        )


        Assertions.assertEquals(
            "2023-12-31 12:34:56  [INFO] - message",
            testLogFile.readLines(Charsets.UTF_8).getOrNull(0) ?: ""
        )

        provider.flush()
        Assertions.assertEquals(
            "2023-12-31 12:34:56  [INFO] - message",
            testLogFile.readLines(Charsets.UTF_8).getOrNull(0)
        )
    }

    @Test
    fun fileProviderTest04(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56Z")

        val provider = FileProvider(testLogFile){
            enableBuffer = true
            bufferSize = 16
            level = LogLevel.DEBUG
            formatter = SimpleTextFormatter
        }

        provider.write(
            name = "default logger",
            msg = "message",
            level = LogLevel.TRACE
        )


        Assertions.assertEquals(
            "",
            testLogFile.readLines(Charsets.UTF_8).getOrNull(0) ?: ""
        )

        provider.flush()
        Assertions.assertEquals(
            "",
            testLogFile.readLines(Charsets.UTF_8).getOrNull(0) ?: ""
        )
    }
    @Test
    fun fileProviderTest05(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56Z")

        val provider = FileProvider(testLogFile){
            enableBuffer = true
            bufferSize = 512
            level = LogLevel.DEBUG
            formatter = DetailTextFormatter
        }

        provider.write(
            name = "default logger",
            msg = "message",
            level = LogLevel.ERROR,
            mapOf("additional" to "additional param")
        )


        Assertions.assertEquals(
            "",
            testLogFile.readLines(Charsets.UTF_8).getOrNull(0) ?: ""
        )

        provider.flush()
        Assertions.assertEquals(
            "2023-12-31T12:34:56Z (${Thread.currentThread().name})[ERROR] - message, additional = {additional=additional param}",
            testLogFile.readLines(Charsets.UTF_8).getOrNull(0) ?: ""
        )
    }
}