package com.milkcocoa.info.colotok.core.provider.builtin

import com.milkcocoa.info.colotok.core.formatter.builtin.structure.DetailStructureFormatter
import com.milkcocoa.info.colotok.core.formatter.builtin.structure.SimpleStructureFormatter
import com.milkcocoa.info.colotok.core.formatter.builtin.structure.StructureFormatterTest
import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.formatter.builtin.text.SimpleTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.util.color.AnsiColor
import com.milkcocoa.info.colotok.util.color.Color
import com.milkcocoa.info.colotok.util.color.ColorExtension.blue
import com.milkcocoa.info.colotok.util.color.ColorExtension.magenta
import com.milkcocoa.info.colotok.util.color.ColorExtension.red
import com.milkcocoa.info.colotok.util.std.StdIn
import com.milkcocoa.info.colotok.util.std.StdOut
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ConsoleProviderTest {
    private val stdIn = StdIn()
    private val stdOut = StdOut()

    @BeforeEach
    public fun before(){
        System.setIn(stdIn)
        System.setOut(stdOut)
    }

    @AfterEach
    public fun after(){
        System.setIn(null)
        System.setOut(null)

        unmockkAll()
    }


    @Test
    fun consoleProviderTest01(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56+09:00")

        val provider = ConsoleProvider{
            formatter = SimpleTextFormatter
            colorize = false
            level = LogLevel.DEBUG
        }

        provider.write(
            name = "default logger",
            msg = "message",
            level = LogLevel.TRACE
        )
        Assertions.assertEquals(
            "",
            stdOut.readLine() ?: ""
        )
    }


    @Test
    fun consoleProviderTest02(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56+09:00")

        val provider = ConsoleProvider{
            formatter = SimpleTextFormatter
            colorize = false
            level = LogLevel.DEBUG
        }

        provider.write(
            name = "default logger",
            msg = "message",
            level = LogLevel.INFO
        )
        Assertions.assertEquals(
            "2023-12-31 12:34:56  [INFO] - message",
            stdOut.readLine() ?: ""
        )
    }


    @Test
    fun consoleProviderTest03(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56+09:00")

        val provider = ConsoleProvider{
            formatter = SimpleTextFormatter
            colorize = true
            level = LogLevel.DEBUG
            infoLevelColor = AnsiColor.BLUE
        }

        provider.write(
            name = "default logger",
            msg = "message",
            level = LogLevel.INFO
        )
        Assertions.assertEquals(
            Color.foreground("2023-12-31 12:34:56  [INFO] - message", AnsiColor.BLUE),
            stdOut.readLine() ?: ""
        )
    }

    @Test
    fun consoleProviderTest04(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56+09:00")

        val provider = ConsoleProvider{
            formatter = DetailTextFormatter
            colorize = true
            level = LogLevel.DEBUG
            infoLevelColor = AnsiColor.BLUE
        }

        provider.write(
            name = "default logger",
            msg = "message",
            level = LogLevel.INFO,
            attr = mapOf("additional" to "additional param")
        )
        Assertions.assertEquals(
            Color.foreground("2023-12-31T12:34:56+09:00 (${Thread.currentThread().name})[INFO] - message, additional = {additional=additional param}", AnsiColor.BLUE),
            stdOut.readLine() ?: ""
        )
    }
    @Test
    fun consoleProviderTest05(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56+09:00")

        val provider = ConsoleProvider{
            formatter = DetailTextFormatter
            colorize = true
            level = LogLevel.DEBUG
            infoLevelColor = AnsiColor.BLUE
        }

        provider.write(
            name = "default logger",
            msg = "message",
            level = LogLevel.TRACE,
            attr = mapOf("additional" to "additional param")
        )
        Assertions.assertEquals(
            "",
            stdOut.readLine() ?: ""
        )
    }
    @Test
    fun consoleProviderTest06(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56+09:00")

        val provider = ConsoleProvider{
            formatter = DetailTextFormatter
            colorize = false
            level = LogLevel.DEBUG
        }

        provider.write(
            name = "default logger",
            msg = "message",
            level = LogLevel.INFO,
            attr = mapOf("additional" to "additional param")
        )
        Assertions.assertEquals(
            "2023-12-31T12:34:56+09:00 (${Thread.currentThread().name})[INFO] - message, additional = {additional=additional param}",
            stdOut.readLine() ?: ""
        )
    }


    @Serializable
    class LogDetail(val scope: String, val message: String): LogStructure
    @Serializable
    class Log(val name: String, val logDetail: LogDetail): LogStructure

    @Test
    fun consoleProviderTest07(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56+09:00")

        val provider = ConsoleProvider{
            formatter = SimpleStructureFormatter
            colorize = false
            level = LogLevel.DEBUG
        }

        provider.write(
            name = "default logger",
            msg = Log(
                name = "range error",
                logDetail = LogDetail(
                    scope = "arg",
                    message = "illegal argument"
                )
            ),
            serializer = Log.serializer(),
            level = LogLevel.INFO
        )
        Assertions.assertEquals(
            """
                {
                "name":"range error",
                "logDetail.scope":"arg",
                "logDetail.message":"illegal argument",
                "level":"INFO",
                "date":"2023-12-31"
                }
            """.trimIndent().replace("\n", ""),
            stdOut.readLine() ?: ""
        )
    }


    @Test
    fun consoleProviderTest08(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56+09:00")

        val provider = ConsoleProvider{
            formatter = DetailStructureFormatter
            colorize = false
            level = LogLevel.DEBUG
        }

        provider.write(
            name = "default logger",
            msg = Log(
                name = "range error",
                logDetail = LogDetail(
                    scope = "arg",
                    message = "illegal argument"
                )
            ),
            serializer = Log.serializer(),
            level = LogLevel.INFO,
            attr = mapOf("attr" to "attributes")
        )
        Assertions.assertEquals(
            """
                {
                "name":"range error",
                "logDetail.scope":"arg",
                "logDetail.message":"illegal argument",
                "level":"INFO",
                "thread":"${Thread.currentThread().name}",
                "attr":"attributes",
                "date":"2023-12-31T12:34:56+09:00"
                }
            """.trimIndent().replace("\n", ""),
            stdOut.readLine() ?: ""
        )
    }


    @Test
    fun consoleProviderTest09(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56+09:00")

        val provider = ConsoleProvider{
            formatter = DetailStructureFormatter
            colorize = false
            level = LogLevel.OFF
        }

        provider.write(
            name = "default logger",
            msg = Log(
                name = "range error",
                logDetail = LogDetail(
                    scope = "arg",
                    message = "illegal argument"
                )
            ),
            serializer = Log.serializer(),
            level = LogLevel.INFO,
            attr = mapOf("attr" to "attributes")
        )
        Assertions.assertEquals(
            "",
            stdOut.readLine() ?: ""
        )
    }


    @Test
    fun consoleProviderTest10(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56+09:00")

        val provider = ConsoleProvider{
            formatter = DetailStructureFormatter
            colorize = true
            level = LogLevel.DEBUG

            warnLevelColor = AnsiColor.MAGENTA
        }

        provider.write(
            name = "default logger",
            msg = Log(
                name = "range error",
                logDetail = LogDetail(
                    scope = "arg",
                    message = "illegal argument"
                )
            ),
            serializer = Log.serializer(),
            level = LogLevel.WARN,
            attr = mapOf("attr" to "attributes")
        )
        Assertions.assertEquals(
            """
                {
                "name":"range error",
                "logDetail.scope":"arg",
                "logDetail.message":"illegal argument",
                "level":"WARN",
                "thread":"${Thread.currentThread().name}",
                "attr":"attributes",
                "date":"2023-12-31T12:34:56+09:00"
                }
            """.trimIndent().replace("\n", "").magenta(),
            stdOut.readLine() ?: ""
        )
    }



    @Test
    fun consoleProviderTest11(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56+09:00")

        val provider = ConsoleProvider{
            formatter = SimpleStructureFormatter
            colorize = true
            level = LogLevel.DEBUG

            warnLevelColor = AnsiColor.MAGENTA
        }

        provider.write(
            name = "default logger",
            msg = "message",
            level = LogLevel.WARN
        )
        Assertions.assertEquals(
            """
                {
                "msg":"message",
                "level":"WARN",
                "date":"2023-12-31"
                }
            """.trimIndent().replace("\n", "").magenta(),
            stdOut.readLine() ?: ""
        )
    }



    @Test
    fun consoleProviderTest12(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56+09:00")

        val provider = ConsoleProvider{
            formatter = SimpleStructureFormatter
            colorize = true
            level = LogLevel.DEBUG

            errorLevelColor = AnsiColor.RED
        }

        provider.write(
            name = "default logger",
            msg = "message",
            level = LogLevel.ERROR,
            attr = mapOf("attr" to "attributes")
        )
        Assertions.assertEquals(
            """
                {
                "msg":"message",
                "level":"ERROR",
                "date":"2023-12-31"
                }
            """.trimIndent().replace("\n", "").red(),
            stdOut.readLine() ?: ""
        )
    }



    @Test
    fun consoleProviderTest13(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56+09:00")

        val provider = ConsoleProvider{
            formatter = DetailStructureFormatter
            colorize = true
            level = LogLevel.DEBUG

            errorLevelColor = AnsiColor.RED
        }

        provider.write(
            name = "default logger",
            msg = "message",
            level = LogLevel.ERROR,
            attr = mapOf("attr" to "attributes")
        )
        Assertions.assertEquals(
            """
                {
                "msg":"message",
                "level":"ERROR",
                "thread":"${Thread.currentThread().name}",
                "attr":"attributes",
                "date":"2023-12-31T12:34:56+09:00"
                }
            """.trimIndent().replace("\n", "").red(),
            stdOut.readLine() ?: ""
        )
    }
}