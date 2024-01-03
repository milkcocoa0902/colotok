package com.milkcocoa.info.colotok.core.formatter.builtin.text

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.util.std.StdIn
import com.milkcocoa.info.colotok.util.std.StdOut
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

object PlainTextFormatterTest {
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
    }

    @Test
    fun plainTextFormatterTest01(){
        val formatter = PlainTextFormatter
        Assertions.assertTrue {
            formatter.format("message", LogLevel.ERROR).equals(
                "message"
            )
        }
    }

    @Test
    fun plainTextFormatterTest02(){
        val formatter = PlainTextFormatter
        Assertions.assertTrue {
            formatter.format(
                "message",
                LogLevel.ERROR,
                mapOf("additional" to "additional parameter")
            ).equals(
                "message"
            )
        }
    }
}