package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.serialization.KSerializer
import org.slf4j.LoggerFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColotokLogger4JTest {

    private class TestProvider : Provider {
        var lastName: String? = null
        var lastMsg: String? = null
        var lastLevel: Level? = null
        var lastAttr: Map<String, String>? = null

        override fun write(record: LogRecord) {
            lastName = record.name
            lastLevel = record.level
            lastAttr = record.attr
            when(record){
                is LogRecord.PlainText -> {
                    lastMsg = record.msg
                }
                is LogRecord.StructuredText<*> -> {
                    lastMsg = record.msg.toString()
                }
            }
        }
    }

    private lateinit var provider: TestProvider
    private lateinit var originalDefault: ColotokLoggerContext

    @BeforeTest
    fun setup() {
        originalDefault = ColotokLoggerContext.DEFAULT
        provider = TestProvider()
        val ctx = ColotokLoggerContext()
            .addProvider(provider)
            .withAttrs(mapOf("base" to "attr"))
        ColotokLoggerContext.setDefault(ctx)
    }

    @AfterTest
    fun tearDown() {
        ColotokLoggerContext.setDefault(originalDefault)
    }

    @Test
    fun info_logs_are_delegated_with_default_attrs_and_logger_name() {
        val logger = LoggerFactory.getLogger("slf4j-test")

        logger.info("hello world")

        assertEquals("slf4j-test", provider.lastName)
        assertEquals("hello world", provider.lastMsg)
        assertEquals(LogLevel.INFO, provider.lastLevel)
        val attrs = provider.lastAttr ?: emptyMap()
        assertEquals("attr", attrs["base"])
        assertEquals("slf4j-test", attrs["logger"])
    }

    @Test
    fun format_overloads_work() {
        val logger = LoggerFactory.getLogger("format-test")

        logger.debug("value=%s", "A")

        assertEquals(LogLevel.DEBUG, provider.lastLevel)
        assertEquals("value=A", provider.lastMsg)
        assertEquals("format-test", provider.lastName)
        assertEquals("format-test", provider.lastAttr?.get("logger"))
    }

    @Test
    fun throwable_is_added_as_attribute() {
        val logger = LoggerFactory.getLogger("throwable-test")
        val ex = IllegalArgumentException("boom")

        logger.error("oops", ex)

        assertEquals(LogLevel.ERROR, provider.lastLevel)
        assertEquals("oops", provider.lastMsg)
        val attrs = provider.lastAttr ?: emptyMap()
        assertTrue(attrs.containsKey("cause"))
        assertTrue(attrs["cause"]!!.contains("IllegalArgumentException"))
        assertEquals("throwable-test", attrs["logger"])
    }
}
