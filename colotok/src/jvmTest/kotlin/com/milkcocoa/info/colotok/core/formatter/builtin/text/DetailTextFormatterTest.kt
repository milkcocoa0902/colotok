package com.milkcocoa.info.colotok.core.formatter.builtin.text

import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.util.ThreadWrapper
import com.milkcocoa.info.colotok.util.std.StdIn
import com.milkcocoa.info.colotok.util.std.StdOut
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class DetailTextFormatterTest {
    private val stdIn = StdIn()
    private val stdOut = StdOut()
    private var originalIn = System.`in`
    private var originalOut = System.out

    @BeforeEach
    public fun before() {
        mockkObject(kotlin.time.Clock.System)
        every { kotlin.time.Clock.System.now() } returns Instant.parse("2023-12-31T12:34:56Z")

        originalIn = System.`in`
        originalOut = System.out
        System.setIn(stdIn)
        System.setOut(stdOut)
    }

    @AfterEach
    public fun after() {
        System.setIn(originalIn)
        System.setOut(originalOut)

        unmockkAll()
    }

    @Test
    fun detailTextFormatterTest01() {
        val formatter = DetailTextFormatter
        Assertions.assertTrue {
            formatter.format(LogRecord.PlainText(name = "test", msg = "message", level = LogLevel.ERROR, attr = emptyMap())).equals(
                "2023-12-31T12:34:56 (${ThreadWrapper.getCurrentThreadName()})[ERROR] - message, additional = {}"
            )
        }
    }

    @Test
    fun detailTextFormatterTest02() {
        val formatter = DetailTextFormatter

        Assertions.assertTrue {
            formatter.format(LogRecord.PlainText(name = "test", msg = "message", level = LogLevel.ERROR, attr = emptyMap())).also {
                println(it)
            }.equals(
                "2023-12-31T12:34:56 (${ThreadWrapper.getCurrentThreadName()})[ERROR] - message, additional = {}"
            )
        }
    }

    @Test
    fun detailTextFormatterTest03() {
        val formatter = DetailTextFormatter

        Assertions.assertTrue {
            formatter.format(
                LogRecord.PlainText(
                    name = "test",
                    msg = "message",
                    level = LogLevel.ERROR,
                    attr = mapOf("additional" to "additional param")
                )
            ).equals(
                @Suppress("ktlint:standard:max-line-length")
                "2023-12-31T12:34:56 (${ThreadWrapper.getCurrentThreadName()})[ERROR] - message, additional = {additional=additional param}"
            )
        }
    }
}