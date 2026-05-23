package com.milkcocoa.info.colotok.core.formatter.builtin.text

import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.util.std.StdIn
import com.milkcocoa.info.colotok.util.std.StdOut
import io.mockk.every
import io.mockk.mockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Instant

object PlainTextFormatterTest {
    private val stdIn = StdIn()
    private val stdOut = StdOut()

    @BeforeEach
    public fun before() {
        mockkObject(kotlin.time.Clock.System)
        every { kotlin.time.Clock.System.now() } returns Instant.parse("2023-12-31T12:34:56Z")
        System.setIn(stdIn)
        System.setOut(stdOut)
    }

    @AfterEach
    public fun after() {
        System.setIn(null)
        System.setOut(null)
    }

    @Test
    fun plainTextFormatterTest01() {
        val formatter = PlainTextFormatter
        Assertions.assertTrue {
            formatter.format(LogRecord.PlainText(name = "test", msg = "message", level = LogLevel.ERROR, attr = emptyMap())).equals(
                "message"
            )
        }
    }

    @Test
    fun plainTextFormatterTest02() {
        val formatter = PlainTextFormatter
        Assertions.assertTrue {
            formatter.format(
                LogRecord.PlainText(
                    name = "test",
                    msg = "message",
                    level = LogLevel.ERROR,
                    attr = mapOf("additional" to "additional parameter")
                )
            ).equals(
                "message"
            )
        }
    }
}