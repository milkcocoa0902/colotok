package com.milkcocoa.info.colotok.core.formatter.builtin.structure

import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.LogLevel
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

object StructureFormatterTest {
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
    fun structureFormatterTest01(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56Z")

        val formatter = SimpleStructureFormatter
        Assertions.assertEquals(
            """
                {
                "msg":"message",
                "level":"INFO",
                "date":"2023-12-31"
                }
            """.trimIndent().replace("\n", ""),
            formatter.format(msg = "message", level = LogLevel.INFO)
        )
    }


    @Test
    fun structureFormatterTest02(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56Z")

        val formatter = SimpleStructureFormatter
        Assertions.assertEquals(
            """
                {
                "msg":"message",
                "level":"WARN",
                "date":"2023-12-31"
                }
            """.trimIndent().replace("\n", ""),
            formatter.format(msg = "message", level = LogLevel.WARN, attrs = mapOf())
        )
    }


    @Test
    fun structureFormatterTest03(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56Z")

        val formatter = DetailStructureFormatter
        Assertions.assertEquals(
            """
                {
                "msg":"message",
                "level":"ERROR",
                "thread":"${Thread.currentThread().name}",
                "date":"2023-12-31T12:34:56Z"
                }
            """.trimIndent().replace("\n", ""),
            formatter.format(msg = "message", level = LogLevel.ERROR)
        )
    }

    @Test
    fun structureFormatterTest04(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56Z")

        val formatter = DetailStructureFormatter
        Assertions.assertEquals(
            """
                {
                "msg":"message",
                "level":"ERROR",
                "thread":"${Thread.currentThread().name}",
                "attr":"attribute",
                "date":"2023-12-31T12:34:56Z"
                }
            """.trimIndent().replace("\n", ""),
            formatter.format(msg = "message", level = LogLevel.ERROR, attrs = mapOf("attr" to "attribute"))
        )
    }


    @Serializable
    class LogDetail(val scope: String, val message: String): LogStructure
    @Serializable
    class Log(val name: String, val logDetail: LogDetail): LogStructure

    @Test
    fun structureFormatterTest05(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56Z")

        val formatter = SimpleStructureFormatter
        Assertions.assertEquals(
            """
                {
                "name":"range error",
                "logDetail.scope":"arg",
                "logDetail.message":"illegal argument",
                "level":"ERROR",
                "date":"2023-12-31"
                }
            """.trimIndent().replace("\n", ""),
            formatter.format(
                msg = Log(
                    name = "range error",
                    logDetail = LogDetail(
                        scope = "arg",
                        message = "illegal argument"
                    )
                ),
                serializer = Log.serializer(),
                level = LogLevel.ERROR
            )
        )
    }

    @Test
    fun structureFormatterTest06(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56Z")

        val formatter = SimpleStructureFormatter
        Assertions.assertEquals(
            """
                {
                "name":"range error",
                "logDetail.scope":"arg",
                "logDetail.message":"illegal argument",
                "level":"ERROR",
                "date":"2023-12-31"
                }
            """.trimIndent().replace("\n", ""),
            formatter.format(
                msg = Log(
                    name = "range error",
                    logDetail = LogDetail(
                        scope = "arg",
                        message = "illegal argument"
                    )
                ),
                serializer = Log.serializer(),
                level = LogLevel.ERROR,
                attrs = mapOf("attr" to "attribute")
            )
        )
    }

    @Test
    fun structureFormatterTest07(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56Z")

        val formatter = DetailStructureFormatter
        Assertions.assertEquals(
            """
                {
                "name":"range error",
                "logDetail.scope":"arg",
                "logDetail.message":"illegal argument",
                "level":"ERROR",
                "thread":"${Thread.currentThread().name}",
                "date":"2023-12-31T12:34:56Z"
                }
            """.trimIndent().replace("\n", ""),
            formatter.format(
                msg = Log(
                    name = "range error",
                    logDetail = LogDetail(
                        scope = "arg",
                        message = "illegal argument"
                    )
                ),
                serializer = Log.serializer(),
                level = LogLevel.ERROR
            )
        )
    }

    @Test
    fun structureFormatterTest08(){
        mockkStatic(ZonedDateTime::class)
        every { ZonedDateTime.now(ZoneId.systemDefault()) } returns ZonedDateTime.parse("2023-12-31T12:34:56Z")

        val formatter = DetailStructureFormatter
        Assertions.assertEquals(
            """
                {
                "name":"range error",
                "logDetail.scope":"arg",
                "logDetail.message":"illegal argument",
                "level":"INFO",
                "thread":"${Thread.currentThread().name}",
                "attr":"attribute",
                "date":"2023-12-31T12:34:56Z"
                }
            """.trimIndent().replace("\n", ""),
            formatter.format(
                msg = Log(
                    name = "range error",
                    logDetail = LogDetail(
                        scope = "arg",
                        message = "illegal argument"
                    )
                ),
                serializer = Log.serializer(),
                level = LogLevel.INFO,
                attrs = mapOf("attr" to "attribute")
            )
        )
    }
}