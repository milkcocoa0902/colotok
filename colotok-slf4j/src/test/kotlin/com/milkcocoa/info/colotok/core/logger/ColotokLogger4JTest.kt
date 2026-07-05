package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.helpers.BasicMarkerFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColotokLogger4JTest {
    private class TestProvider : Provider(
        config =
            ConsoleProviderConfig().apply {
                level = LogLevel.TRACE
            }
    ) {
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

            assertLastLog(
                expectedName = "slf4j-test",
                expectedMessage = "hello world",
                expectedLevel = LogLevel.INFO
            )
        }
    }

    @Test
    fun format_overloads_work_with_slf4j_placeholders() {
        runBlocking {
            val logger = LoggerFactory.getLogger("format-test")

            logger.debug("value={}", "A")

            assertLastLog(
                expectedName = "format-test",
                expectedMessage = "value=A",
                expectedLevel = LogLevel.DEBUG
            )
        }
    }

    @Test
    fun multiple_placeholders_format_in_order() {
        runBlocking {
            val logger = LoggerFactory.getLogger("format-multiple-test")

            logger.info("left={} right={}", "A", "B")

            assertLastLog(
                expectedName = "format-multiple-test",
                expectedMessage = "left=A right=B",
                expectedLevel = LogLevel.INFO
            )
        }
    }

    @Test
    fun trailing_throwable_argument_is_added_as_attribute() {
        runBlocking {
            val logger = LoggerFactory.getLogger("throwable-argument-test")
            val ex = IllegalArgumentException("boom")

            logger.warn("failed {}", "save", ex)

            assertLastLog(
                expectedName = "throwable-argument-test",
                expectedMessage = "failed save",
                expectedLevel = LogLevel.WARN
            ) { attrs ->
                assertTrue(attrs.containsKey("cause"))
                assertTrue(attrs["cause"]!!.contains("IllegalArgumentException"))
            }
        }
    }

    @Test
    fun throwable_is_added_as_attribute() {
        runBlocking {
            val logger = LoggerFactory.getLogger("throwable-test")
            val ex = IllegalArgumentException("boom")

            logger.error("oops", ex)

            assertLastLog(
                expectedName = "throwable-test",
                expectedMessage = "oops",
                expectedLevel = LogLevel.ERROR
            ) { attrs ->
                assertTrue(attrs.containsKey("cause"))
                assertTrue(attrs["cause"]!!.contains("IllegalArgumentException"))
            }
        }
    }

    @Test
    fun enabled_checks_return_true_for_plain_and_marker_variants() {
        val logger = LoggerFactory.getLogger("enabled-test")
        val marker = marker()

        assertTrue(logger.isTraceEnabled)
        assertTrue(logger.isDebugEnabled)
        assertTrue(logger.isInfoEnabled)
        assertTrue(logger.isWarnEnabled)
        assertTrue(logger.isErrorEnabled)
        assertTrue(logger.isTraceEnabled(marker))
        assertTrue(logger.isDebugEnabled(marker))
        assertTrue(logger.isInfoEnabled(marker))
        assertTrue(logger.isWarnEnabled(marker))
        assertTrue(logger.isErrorEnabled(marker))
    }

    @Test
    fun marker_message_overloads_delegate_to_normal_logging() {
        runBlocking {
            val logger = LoggerFactory.getLogger("marker-message-test")
            val marker = marker()

            logger.trace(marker, "trace message")
            assertLastLog("marker-message-test", "trace message", LogLevel.TRACE)

            logger.debug(marker, "debug message")
            assertLastLog("marker-message-test", "debug message", LogLevel.DEBUG)

            logger.info(marker, "info message")
            assertLastLog("marker-message-test", "info message", LogLevel.INFO)

            logger.warn(marker, "warn message")
            assertLastLog("marker-message-test", "warn message", LogLevel.WARN)

            logger.error(marker, "error message")
            assertLastLog("marker-message-test", "error message", LogLevel.ERROR)
        }
    }

    @Test
    fun marker_parameterized_overloads_use_slf4j_formatting() {
        runBlocking {
            val logger = LoggerFactory.getLogger("marker-format-test")
            val marker = marker()

            logger.info(marker, "single={}", "A")
            assertLastLog("marker-format-test", "single=A", LogLevel.INFO)

            logger.debug(marker, "left={} right={}", "A", "B")
            assertLastLog("marker-format-test", "left=A right=B", LogLevel.DEBUG)

            logger.warn(marker, "items={} {} {}", "A", "B", "C")
            assertLastLog("marker-format-test", "items=A B C", LogLevel.WARN)
        }
    }

    @Test
    fun marker_throwable_overloads_add_cause_attribute() {
        runBlocking {
            val logger = LoggerFactory.getLogger("marker-throwable-test")
            val marker = marker()
            val explicit = IllegalArgumentException("explicit")
            val trailing = IllegalStateException("trailing")

            logger.error(marker, "explicit failure", explicit)
            assertLastLog(
                expectedName = "marker-throwable-test",
                expectedMessage = "explicit failure",
                expectedLevel = LogLevel.ERROR
            ) { attrs ->
                assertTrue(attrs.containsKey("cause"))
                assertTrue(attrs["cause"]!!.contains("IllegalArgumentException"))
            }

            logger.trace(marker, "trailing {}", "failure", trailing)
            assertLastLog(
                expectedName = "marker-throwable-test",
                expectedMessage = "trailing failure",
                expectedLevel = LogLevel.TRACE
            ) { attrs ->
                assertTrue(attrs.containsKey("cause"))
                assertTrue(attrs["cause"]!!.contains("IllegalStateException"))
            }
        }
    }

    private fun marker(): Marker = BasicMarkerFactory().getMarker("ignored")

    private suspend fun assertLastLog(
        expectedName: String,
        expectedMessage: String,
        expectedLevel: Level,
        assertAttrs: (Map<String, String>) -> Unit = {}
    ) {
        provider.flush()
        assertEquals(expectedName, provider.lastName)
        assertEquals(expectedMessage, provider.lastMsg)
        assertEquals(expectedLevel, provider.lastLevel)
        val attrs = provider.lastAttr ?: emptyMap()
        assertEquals("attr", attrs["base"])
        assertEquals(expectedName, attrs["logger"])
        assertAttrs(attrs)
    }
}