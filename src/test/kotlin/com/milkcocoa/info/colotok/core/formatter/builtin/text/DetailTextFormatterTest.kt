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

object DetailTextFormatterTest {
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
    fun detailTextFormatterTest01(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56Z")


        val formatter = DetailTextFormatter
        Assertions.assertTrue {
            formatter.format("message", LogLevel.ERROR).equals(
                "2023-12-31T12:34:56Z (Test worker)[ERROR] - message, additional = {}"
            )
        }
    }


    @Test
    fun detailTextFormatterTest02(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56+09:00")


        val formatter = DetailTextFormatter

        Assertions.assertTrue {
            formatter.format("message", LogLevel.ERROR).equals(
                "2023-12-31T12:34:56+09:00 (Test worker)[ERROR] - message, additional = {}"
            )
        }
    }

    @Test
    fun detailTextFormatterTest03() {
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56+09:00")

        val formatter = DetailTextFormatter

        Assertions.assertTrue {
            formatter.format(
                "message",
                LogLevel.ERROR,
                mapOf("additional" to "additional param")
            ).equals(
                "2023-12-31T12:34:56+09:00 (Test worker)[ERROR] - message, additional = {additional=additional param}"
            )
        }
    }
}