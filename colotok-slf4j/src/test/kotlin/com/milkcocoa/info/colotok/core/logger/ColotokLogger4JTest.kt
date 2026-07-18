package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.formatter.details.TextFormatter
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColotokLogger4JTest {
    private class TestProvider(level: Level = LogLevel.TRACE) : Provider(
        config =
            ConsoleProviderConfig().apply {
                this.level = level
            }
    ) {
        var lastName: String? = null
        var lastMsg: String? = null
        var lastLevel: Level? = null
        var lastAttr: Map<String, String>? = null
        var lastCaller: String? = null

        override suspend fun onMessage(record: LogRecord) {
            lastName = record.name
            lastLevel = record.level
            lastAttr = record.attr
            lastCaller = record.format(object : TextFormatter("${Element.CALLER}") {})
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
    private val providersToClose = mutableListOf<TestProvider>()

    @BeforeTest
    fun setup() {
        originalDefault = ColotokLoggerContext.DEFAULT
        provider = testProvider()
        val ctx =
            ColotokLoggerContext()
                .addProvider(provider)
                .withAttrs(mapOf("base" to "attr"))
        ColotokLoggerContext.setDefault(ctx)
    }

    @AfterTest
    fun tearDown() {
        ColotokLoggerContext.setDefault(originalDefault)
        providersToClose.forEach(TestProvider::forceShutdown)
        providersToClose.clear()
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
    fun caller_points_to_the_slf4j_user_call_site() = runBlocking {
        val logger = LoggerFactory.getLogger("caller-test")

        logger.info("message")
        provider.flush()

        assertTrue(provider.lastCaller!!.contains("caller_points_to_the_slf4j_user_call_site"), provider.lastCaller)
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
    fun enabled_reflects_any_provider_threshold() {
        val infoProvider = testProvider(LogLevel.INFO)
        val errorProvider = testProvider(LogLevel.ERROR)
        ColotokLoggerContext.setDefault(
            ColotokLoggerContext()
                .addProvider(infoProvider)
                .addProvider(errorProvider)
        )
        val logger = LoggerFactory.getLogger("enabled-threshold-test")
        val marker = marker()

        assertFalse(logger.isTraceEnabled)
        assertFalse(logger.isDebugEnabled)
        assertTrue(logger.isInfoEnabled)
        assertTrue(logger.isWarnEnabled)
        assertTrue(logger.isErrorEnabled)
        assertFalse(logger.isTraceEnabled(marker))
        assertFalse(logger.isDebugEnabled(marker))
        assertTrue(logger.isInfoEnabled(marker))
        assertTrue(logger.isWarnEnabled(marker))
        assertTrue(logger.isErrorEnabled(marker))
    }

    @Test
    fun enabled_is_false_without_eligible_providers() {
        ColotokLoggerContext.setDefault(ColotokLoggerContext())
        val noProviders = LoggerFactory.getLogger("enabled-empty-test")

        assertFalse(noProviders.isErrorEnabled)

        ColotokLoggerContext.setDefault(
            ColotokLoggerContext().addProvider(testProvider(LogLevel.OFF))
        )
        val offProvider = LoggerFactory.getLogger("enabled-off-test")

        assertFalse(offProvider.isTraceEnabled)
        assertFalse(offProvider.isDebugEnabled)
        assertFalse(offProvider.isInfoEnabled)
        assertFalse(offProvider.isWarnEnabled)
        assertFalse(offProvider.isErrorEnabled)
    }

    @Test
    fun all_levels_support_slf4j_placeholder_overloads() {
        runBlocking {
            val logger = LoggerFactory.getLogger("overload-contract-test")
            val marker = marker()

            BridgeLevel.entries.forEach { level ->
                logPlain(logger, level, "plain")
                assertLastLog("overload-contract-test", "plain", level.colotok)

                logOne(logger, level, "one={}", "A")
                assertLastLog("overload-contract-test", "one=A", level.colotok)

                logTwo(logger, level, "two={} {}", "A", "B")
                assertLastLog("overload-contract-test", "two=A B", level.colotok)

                logVararg(logger, level, "many={} {} {}", arrayOf("A", "B", "C"))
                assertLastLog("overload-contract-test", "many=A B C", level.colotok)

                val explicit = IllegalArgumentException("explicit", IllegalStateException("root"))
                logThrowable(logger, level, "explicit failure", explicit)
                assertLastLog("overload-contract-test", "explicit failure", level.colotok) {
                    assertFullStackTrace(it.getValue("cause"), explicit)
                }

                val trailing = IllegalArgumentException("trailing", IllegalStateException("root"))
                logVararg(logger, level, "trailing {}", arrayOf("failure", trailing))
                assertLastLog("overload-contract-test", "trailing failure", level.colotok) {
                    assertFullStackTrace(it.getValue("cause"), trailing)
                }

                logMarker(logger, level, marker, "marker")
                assertLastLog("overload-contract-test", "marker", level.colotok)

                logOne(logger, level, "escaped=\\{}", "ignored")
                assertLastLog("overload-contract-test", "escaped={}", level.colotok)
            }
        }
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

    private fun testProvider(level: Level = LogLevel.TRACE): TestProvider =
        TestProvider(level).also(providersToClose::add)

    private enum class BridgeLevel(val colotok: Level) {
        TRACE(LogLevel.TRACE),
        DEBUG(LogLevel.DEBUG),
        INFO(LogLevel.INFO),
        WARN(LogLevel.WARN),
        ERROR(LogLevel.ERROR),
    }

    private fun logPlain(
        logger: org.slf4j.Logger,
        level: BridgeLevel,
        message: String?
    ) {
        when (level) {
            BridgeLevel.TRACE -> logger.trace(message)
            BridgeLevel.DEBUG -> logger.debug(message)
            BridgeLevel.INFO -> logger.info(message)
            BridgeLevel.WARN -> logger.warn(message)
            BridgeLevel.ERROR -> logger.error(message)
        }
    }

    private fun logOne(
        logger: org.slf4j.Logger,
        level: BridgeLevel,
        pattern: String?,
        argument: Any?
    ) {
        when (level) {
            BridgeLevel.TRACE -> logger.trace(pattern, argument)
            BridgeLevel.DEBUG -> logger.debug(pattern, argument)
            BridgeLevel.INFO -> logger.info(pattern, argument)
            BridgeLevel.WARN -> logger.warn(pattern, argument)
            BridgeLevel.ERROR -> logger.error(pattern, argument)
        }
    }

    private fun logTwo(
        logger: org.slf4j.Logger,
        level: BridgeLevel,
        pattern: String?,
        first: Any?,
        second: Any?
    ) {
        when (level) {
            BridgeLevel.TRACE -> logger.trace(pattern, first, second)
            BridgeLevel.DEBUG -> logger.debug(pattern, first, second)
            BridgeLevel.INFO -> logger.info(pattern, first, second)
            BridgeLevel.WARN -> logger.warn(pattern, first, second)
            BridgeLevel.ERROR -> logger.error(pattern, first, second)
        }
    }

    private fun logVararg(
        logger: org.slf4j.Logger,
        level: BridgeLevel,
        pattern: String?,
        arguments: Array<out Any?>
    ) {
        when (level) {
            BridgeLevel.TRACE -> logger.trace(pattern, *arguments)
            BridgeLevel.DEBUG -> logger.debug(pattern, *arguments)
            BridgeLevel.INFO -> logger.info(pattern, *arguments)
            BridgeLevel.WARN -> logger.warn(pattern, *arguments)
            BridgeLevel.ERROR -> logger.error(pattern, *arguments)
        }
    }

    private fun logThrowable(
        logger: org.slf4j.Logger,
        level: BridgeLevel,
        message: String?,
        throwable: Throwable
    ) {
        when (level) {
            BridgeLevel.TRACE -> logger.trace(message, throwable)
            BridgeLevel.DEBUG -> logger.debug(message, throwable)
            BridgeLevel.INFO -> logger.info(message, throwable)
            BridgeLevel.WARN -> logger.warn(message, throwable)
            BridgeLevel.ERROR -> logger.error(message, throwable)
        }
    }

    private fun logMarker(
        logger: org.slf4j.Logger,
        level: BridgeLevel,
        marker: Marker,
        message: String?
    ) {
        when (level) {
            BridgeLevel.TRACE -> logger.trace(marker, message)
            BridgeLevel.DEBUG -> logger.debug(marker, message)
            BridgeLevel.INFO -> logger.info(marker, message)
            BridgeLevel.WARN -> logger.warn(marker, message)
            BridgeLevel.ERROR -> logger.error(marker, message)
        }
    }

    private fun assertFullStackTrace(actual: String, throwable: Throwable) {
        assertTrue(actual.contains(throwable::class.simpleName!!))
        assertTrue(actual.contains("at "))
        assertTrue(actual.contains("Caused by:"))
        assertTrue(actual.contains("IllegalStateException: root"))
    }

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
