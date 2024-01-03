package com.milkcocoa.info.colotok.core.provider.builtin

import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.formatter.builtin.text.SimpleTextFormatter
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.util.color.AnsiColor
import com.milkcocoa.info.colotok.util.color.Color
import com.milkcocoa.info.colotok.util.color.ColorExtension.blue
import com.milkcocoa.info.colotok.util.std.StdIn
import com.milkcocoa.info.colotok.util.std.StdOut
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
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
}