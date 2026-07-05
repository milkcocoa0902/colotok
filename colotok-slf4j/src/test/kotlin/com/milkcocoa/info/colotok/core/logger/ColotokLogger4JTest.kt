package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColotokLogger4JTest {
    private class TestProvider : Provider(config = ConsoleProviderConfig()) {
        var lastName: String? = null
        var lastMsg: String? = null
        var lastLevel: Level? = null
        var lastAttr: Map<String, String>? = null

        override suspend fun onMessage(record: LogRecord) {
            lastName = record.name
            lastLevel = record.level
            lastAttr = record.attr
            when (record) {
                is LogRecord.PlainText -> {
                    lastMsg = record.msg
                }
                is LogRecord.StructuredText<*> -> {
                    lastMsg = record.msg.toString()
                }
                is LogRecord.Metrics -> {
                    lastMsg = record.msg
                }
                is LogRecord.Pin -> {}
            }
        }
    }

    private lateinit var provider: TestProvider
    private lateinit var originalDefault: ColotokLoggerContext

    @BeforeTest
    fun setup() {
        originalDefault = ColotokLoggerContext.DEFAULT
        provider = TestProvider()
        val ctx =
            ColotokLoggerContext()
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
        runBlocking {
            val logger = LoggerFactory.getLogger("slf4j-test")

            logger.info("hello world")
            provider.flush()

            assertEquals("slf4j-test", provider.lastName)
            assertEquals("hello world", provider.lastMsg)
            assertEquals(LogLevel.INFO, provider.lastLevel)
            val attrs = provider.lastAttr ?: emptyMap()
            assertEquals("attr", attrs["base"])
            assertEquals("slf4j-test", attrs["logger"])
        }
    }

    @Test
    fun format_overloads_work_with_slf4j_placeholders() {
        runBlocking {
            val logger = LoggerFactory.getLogger("format-test")

            logger.debug("value={}", "A")
            provider.flush()

            assertEquals(LogLevel.DEBUG, provider.lastLevel)
            assertEquals("value=A", provider.lastMsg)
            assertEquals("format-test", provider.lastName)
            assertEquals("format-test", provider.lastAttr?.get("logger"))
        }
    }

    @Test
    fun multiple_placeholders_format_in_order() {
        runBlocking {
            val logger = LoggerFactory.getLogger("format-multiple-test")

            logger.info("left={} right={}", "A", "B")
            provider.flush()

            assertEquals(LogLevel.INFO, provider.lastLevel)
            assertEquals("left=A right=B", provider.lastMsg)
            assertEquals("format-multiple-test", provider.lastName)
            assertEquals("format-multiple-test", provider.lastAttr?.get("logger"))
        }
    }

    @Test
    fun trailing_throwable_argument_is_added_as_attribute() {
        runBlocking {
            val logger = LoggerFactory.getLogger("throwable-argument-test")
            val ex = IllegalArgumentException("boom")

            logger.warn("failed {}", "save", ex)
            provider.flush()

            assertEquals(LogLevel.WARN, provider.lastLevel)
            assertEquals("failed save", provider.lastMsg)
            val attrs = provider.lastAttr ?: emptyMap()
            assertTrue(attrs.containsKey("cause"))
            assertTrue(attrs["cause"]!!.contains("IllegalArgumentException"))
            assertEquals("throwable-argument-test", attrs["logger"])
        }
    }

    @Test
    fun throwable_is_added_as_attribute() {
        runBlocking {
            val logger = LoggerFactory.getLogger("throwable-test")
            val ex = IllegalArgumentException("boom")

            logger.error("oops", ex)
            provider.flush()

            assertEquals(LogLevel.ERROR, provider.lastLevel)
            assertEquals("oops", provider.lastMsg)
            val attrs = provider.lastAttr ?: emptyMap()
            assertTrue(attrs.containsKey("cause"))
            assertTrue(attrs["cause"]!!.contains("IllegalArgumentException"))
            assertEquals("throwable-test", attrs["logger"])
        }
    }
}