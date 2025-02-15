package com.milkcocoa.info.colotok.core.provider.builtin

import com.milkcocoa.info.colotok.core.formatter.builtin.structure.DetailStructureFormatter
import com.milkcocoa.info.colotok.core.formatter.builtin.structure.SimpleStructureFormatter
import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.formatter.builtin.text.SimpleTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.builtin.file.FileProvider
import com.milkcocoa.info.colotok.core.provider.rotation.SizeBaseRotation
import com.milkcocoa.info.colotok.util.ThreadWrapper
import com.milkcocoa.info.colotok.util.unit.Size.KiB
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import okio.Path.Companion.toOkioPath
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.deleteRecursively
import kotlin.io.path.notExists
import kotlin.io.path.pathString
import kotlin.io.path.readLines

@OptIn(InternalSerializationApi::class)
object FileProviderTest {
    val logFilesDir = java.nio.file.Path.of("./log-dir/")
    val testLogFile = java.nio.file.Path.of(logFilesDir.pathString, "junit-test-log.log")

    @BeforeEach
    fun before() {
        mockkObject(Clock.System)
        every { Clock.System.now() } returns Instant.parse("2023-12-31T12:34:56Z")

        if (logFilesDir.notExists()) {
            Files.createDirectory(logFilesDir)
        }
        if (testLogFile.notExists()) {
            testLogFile.createFile()
        }
    }

    @OptIn(ExperimentalPathApi::class)
    @AfterEach
    fun after() {
        logFilesDir.deleteRecursively()
        unmockkAll()
    }

    @Test
    fun fileProviderTest01() {
        val provider =
            FileProvider(testLogFile.toOkioPath()) {
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
    fun fileProviderTest02() {
        val provider =
            FileProvider(testLogFile.toOkioPath()) {
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
    fun fileProviderTest03() {
        val provider =
            FileProvider(testLogFile.toOkioPath()) {
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
    fun fileProviderTest04() {
        val provider =
            FileProvider(testLogFile.toOkioPath()) {
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
    fun fileProviderTest05() {
        val provider =
            FileProvider(testLogFile.toOkioPath()) {
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
        @Suppress("ktlint:standard:max-line-length")
        Assertions.assertEquals(
            "2023-12-31T12:34:56 (${ThreadWrapper.getCurrentThreadName()})[ERROR] - message, additional = {additional=additional param}",
            testLogFile.readLines(Charsets.UTF_8).getOrNull(0) ?: ""
        )
    }

    @Test
    fun fileProviderTest06() {
        val provider =
            FileProvider(testLogFile.toOkioPath()) {
                enableBuffer = true
                bufferSize = 512
                level = LogLevel.DEBUG
                formatter = DetailTextFormatter
                rotation = SizeBaseRotation(16)
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
        Assertions.assertTrue { testLogFile.notExists() }
        val rotatedFile =
            testLogFile.fileName.toString().plus(
                ".1"
            ).let { java.nio.file.Path.of(logFilesDir.pathString, it) }

        @Suppress("ktlint:standard:max-line-length")
        Assertions.assertEquals(
            "2023-12-31T12:34:56 (${ThreadWrapper.getCurrentThreadName()})[ERROR] - message, additional = {additional=additional param}",
            rotatedFile.readLines(Charsets.UTF_8).getOrNull(0) ?: ""
        )
    }

    @Test
    fun fileProviderTest07() {
        val provider =
            FileProvider(testLogFile.toOkioPath()) {
                enableBuffer = true
                bufferSize = 10
                level = LogLevel.DEBUG
                formatter = DetailTextFormatter
                rotation = SizeBaseRotation(16)
            }

        provider.write(
            name = "default logger",
            msg = "message",
            level = LogLevel.ERROR,
            mapOf("additional" to "additional param")
        )

        Assertions.assertTrue { testLogFile.notExists() }
        val rotatedFile =
            testLogFile.fileName.toString().plus(
                ".1"
            ).let { java.nio.file.Path.of(logFilesDir.pathString, it) }
        @Suppress("ktlint:standard:max-line-length")
        Assertions.assertEquals(
            "2023-12-31T12:34:56 (${ThreadWrapper.getCurrentThreadName()})[ERROR] - message, additional = {additional=additional param}",
            rotatedFile.readLines(Charsets.UTF_8).getOrNull(0) ?: ""
        )
    }

    @Serializable
    class LogDetail(val scope: String, val message: String) : LogStructure

    @Serializable
    class Log(val name: String, val logDetail: LogDetail) : LogStructure

    @Test
    fun fileProviderTest08() {
        val provider =
            FileProvider(testLogFile.toOkioPath()) {
                enableBuffer = true
                bufferSize = 512
                level = LogLevel.DEBUG
                formatter = SimpleStructureFormatter
            }

        provider.write(
            name = "default logger",
            msg = "message",
            level = LogLevel.ERROR,
            attr = mapOf("attr" to "attributes")
        )

        Assertions.assertEquals(
            "",
            testLogFile.readLines(Charsets.UTF_8).getOrNull(0) ?: ""
        )

        provider.flush()
        Assertions.assertEquals(
            """
            {
            "message":"message",
            "level":"ERROR",
            "date":"2023-12-31"
            }
            """.trimIndent().replace("\n", ""),
            testLogFile.readLines(Charsets.UTF_8).getOrNull(0) ?: ""
        )
    }

    @Test
    fun fileProviderTest09() {
        val provider =
            FileProvider(testLogFile.toOkioPath()) {
                enableBuffer = false
                bufferSize = 512
                level = LogLevel.DEBUG
                formatter = DetailStructureFormatter
            }

        provider.write(
            name = "default logger",
            msg =
                Log(
                    name = "range error",
                    logDetail =
                        LogDetail(
                            scope = "arg",
                            message = "illegal argument"
                        )
                ),
            serializer = Log::class.serializer(),
            level = LogLevel.WARN,
            attr = mapOf("attr" to "attributes")
        )

        Assertions.assertEquals(
            """
                |{
                    |"message":{
                        |"name":"range error",
                        |"logDetail":{
                            |"scope":"arg",
                            |"message":"illegal argument"
                        |}
                    |},
                    |"level":"WARN",
                    |"thread":"${ThreadWrapper.getCurrentThreadName()}",
                    |"attr":"attributes",
                    |"date":"2023-12-31T12:34:56"
                |}
            """.trimMargin().replace("\n", ""),
            testLogFile.readLines(Charsets.UTF_8).getOrNull(0) ?: ""
        )
    }

    @Test
    fun fileProviderTest10() {
        val provider =
            FileProvider(testLogFile.toOkioPath()) {
                enableBuffer = true
                bufferSize = 512
                level = LogLevel.DEBUG
                formatter = DetailStructureFormatter
            }

        provider.write(
            name = "default logger",
            msg =
                Log(
                    name = "range error",
                    logDetail =
                        LogDetail(
                            scope = "arg",
                            message = "illegal argument"
                        )
                ),
            serializer = Log::class.serializer(),
            level = LogLevel.WARN,
            attr = mapOf("attr" to "attributes")
        )

        Assertions.assertEquals(
            "",
            testLogFile.readLines(Charsets.UTF_8).getOrNull(0) ?: ""
        )

        provider.flush()
        Assertions.assertEquals(
            """
                |{
                    |"message":{
                        |"name":"range error",
                        |"logDetail":{
                            |"scope":"arg",
                            |"message":"illegal argument"
                        |}
                    |},
                    |"level":"WARN",
                    |"thread":"${ThreadWrapper.getCurrentThreadName()}",
                    |"attr":"attributes",
                    |"date":"2023-12-31T12:34:56"
                |}
            """.trimMargin().replace("\n", ""),
            testLogFile.readLines(Charsets.UTF_8).getOrNull(0) ?: ""
        )
    }

    @Test
    fun fileProviderTest11() {
        val provider =
            FileProvider(testLogFile.toOkioPath()) {
                enableBuffer = true
                bufferSize = 32
                level = LogLevel.DEBUG
                formatter = DetailStructureFormatter
            }

        provider.write(
            name = "default logger",
            msg =
                Log(
                    name = "range error",
                    logDetail =
                        LogDetail(
                            scope = "arg",
                            message = "illegal argument"
                        )
                ),
            serializer = Log::class.serializer(),
            level = LogLevel.WARN,
            attr = mapOf("attr" to "attributes")
        )

        Assertions.assertEquals(
            """
                |{
                    |"message":{
                        |"name":"range error",
                        |"logDetail":{
                            |"scope":"arg",
                            |"message":"illegal argument"
                        |}
                    |},
                    |"level":"WARN",
                    |"thread":"${ThreadWrapper.getCurrentThreadName()}",
                    |"attr":"attributes",
                    |"date":"2023-12-31T12:34:56"
                |}
            """.trimMargin().replace("\n", ""),
            testLogFile.readLines(Charsets.UTF_8).getOrNull(0) ?: ""
        )
    }
}