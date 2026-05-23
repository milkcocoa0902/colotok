package com.milkcocoa.info.colotok.core.formatter.builtin.text

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.util.std.StdIn
import com.milkcocoa.info.colotok.util.std.StdOut
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SimpleTextFormatterTest {
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
    fun simpleTextFormatterTest01() {
        val formatter = SimpleTextFormatter
        Assertions.assertTrue {
            formatter.format("message", LogLevel.ERROR).equals(
                "2023-12-31 12:34:56  [ERROR] - message"
            )
        }
    }

    @Test
    fun simpleTextFormatterTest02() {
        val formatter = SimpleTextFormatter

        Assertions.assertTrue {
            formatter.format("message", LogLevel.ERROR).equals(
                "2023-12-31 12:34:56  [ERROR] - message"
            )
        }
    }

    @Test
    fun simpleTextFormatterTest03() {
        val formatter = SimpleTextFormatter

        Assertions.assertTrue {
            formatter.format(
                "message",
                LogLevel.ERROR,
                mapOf("additional" to "additional param")
            ).equals(
                "2023-12-31 12:34:56  [ERROR] - message"
            )
        }
    }
}