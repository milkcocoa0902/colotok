package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.formatter.details.TextFormatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.helpers.BasicMarkerFactory

class ColotokLogger4J2Test {
    private class TestProvider(level: Level = LogLevel.TRACE) : Provider(
        config = ConsoleProviderConfig().apply { this.level = level }
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

    @BeforeEach
    fun setup() {
        originalDefault = ColotokLoggerContext.DEFAULT
        provider = testProvider()
        val ctx =
            ColotokLoggerContext()
                .addProvider(provider)
                .withAttrs(mapOf("base" to "attr"))
        ColotokLoggerContext.setDefault(ctx)
    }

    @AfterEach
    fun tearDown() {
        ColotokLoggerContext.setDefault(originalDefault)
        providersToClose.forEach(TestProvider::forceShutdown)
        providersToClose.clear()
    }

    @Test
    fun logger_name_matches_factory_request() {
        val logger = LoggerFactory.getLogger("logger-name-test-2")

        assertEquals("logger-name-test-2", logger.name)
    }

    @Test
    fun info_logs_are_delegated_with_default_attrs_and_logger_name() {
        runBlocking {
            val logger = LoggerFactory.getLogger("slf4j2-test")

            logger.info("hello world")
            provider.flush()

            assertEquals("slf4j2-test", provider.lastName)
            assertEquals("hello world", provider.lastMsg)
            assertEquals(LogLevel.INFO, provider.lastLevel)
            val attrs = provider.lastAttr ?: emptyMap()
            assertEquals("attr", attrs["base"])
            assertEquals("slf4j2-test", attrs["logger"])
        }
    }

    @Test
    fun caller_points_to_the_slf4j2_user_call_site() = runBlocking {
        val logger = LoggerFactory.getLogger("caller-test-2")

        logger.info("message")
        provider.flush()

        assertTrue(provider.lastCaller!!.contains("caller_points_to_the_slf4j2_user_call_site"), provider.lastCaller)
    }

    @Test
    fun format_overloads_work_with_slf4j_placeholders() {
        runBlocking {
            val logger = LoggerFactory.getLogger("format-test-2")

            logger.debug("value={}", "A")
            provider.flush()

            assertEquals(LogLevel.DEBUG, provider.lastLevel)
            assertEquals("value=A", provider.lastMsg)
            assertEquals("format-test-2", provider.lastName)
            assertEquals("format-test-2", provider.lastAttr?.get("logger"))
        }
    }

    @Test
    fun trailing_throwable_argument_is_added_as_attribute() {
        runBlocking {
            val logger = LoggerFactory.getLogger("throwable-argument-test-2")
            val ex = IllegalArgumentException("boom")

            logger.warn("failed {}", "save", ex)
            provider.flush()

            assertEquals(LogLevel.WARN, provider.lastLevel)
            assertEquals("failed save", provider.lastMsg)
            val attrs = provider.lastAttr ?: emptyMap()
            assertTrue(attrs.containsKey("cause"))
            assertTrue(attrs["cause"]!!.contains("IllegalArgumentException"))
            assertEquals("throwable-argument-test-2", attrs["logger"])
        }
    }

    @Test
    fun throwable_is_added_as_attribute() {
        runBlocking {
            val logger = LoggerFactory.getLogger("throwable-test-2")
            val ex = IllegalArgumentException("boom")

            logger.error("oops", ex)
            provider.flush()

            assertEquals(LogLevel.ERROR, provider.lastLevel)
            assertEquals("oops", provider.lastMsg)
            val attrs = provider.lastAttr ?: emptyMap()
            assertTrue(attrs.containsKey("cause"))
            assertTrue(attrs["cause"]!!.contains("IllegalArgumentException"))
            assertEquals("throwable-test-2", attrs["logger"])
        }
    }

    @Test
    fun enabled_reflects_any_provider_threshold() {
        ColotokLoggerContext.setDefault(
            ColotokLoggerContext()
                .addProvider(testProvider(LogLevel.INFO))
                .addProvider(testProvider(LogLevel.ERROR))
        )
        val logger = LoggerFactory.getLogger("enabled-threshold-test-2")
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
        val noProviders = LoggerFactory.getLogger("enabled-empty-test-2")

        assertFalse(noProviders.isErrorEnabled)

        ColotokLoggerContext.setDefault(
            ColotokLoggerContext().addProvider(testProvider(LogLevel.OFF))
        )
        val offProvider = LoggerFactory.getLogger("enabled-off-test-2")

        assertFalse(offProvider.isTraceEnabled)
        assertFalse(offProvider.isDebugEnabled)
        assertFalse(offProvider.isInfoEnabled)
        assertFalse(offProvider.isWarnEnabled)
        assertFalse(offProvider.isErrorEnabled)
    }

    @Test
    fun all_levels_support_slf4j_placeholder_overloads() {
        runBlocking {
            val logger = LoggerFactory.getLogger("overload-contract-test-2")
            val marker = marker()

            BridgeLevel.entries.forEach { level ->
                logPlain(logger, level, "plain")
                assertLastLog("plain", level.colotok)

                logOne(logger, level, "one={}", "A")
                assertLastLog("one=A", level.colotok)

                logTwo(logger, level, "two={} {}", "A", "B")
                assertLastLog("two=A B", level.colotok)

                logVararg(logger, level, "many={} {} {}", arrayOf("A", "B", "C"))
                assertLastLog("many=A B C", level.colotok)

                val explicit = IllegalArgumentException("explicit", IllegalStateException("root"))
                logThrowable(logger, level, "explicit failure", explicit)
                assertLastLog("explicit failure", level.colotok) {
                    assertFullStackTrace(it.getValue("cause"), explicit)
                }

                val trailing = IllegalArgumentException("trailing", IllegalStateException("root"))
                logVararg(logger, level, "trailing {}", arrayOf("failure", trailing))
                assertLastLog("trailing failure", level.colotok) {
                    assertFullStackTrace(it.getValue("cause"), trailing)
                }

                logMarker(logger, level, marker, "marker")
                assertLastLog("marker", level.colotok)

                logOne(logger, level, "escaped=\\{}", "ignored")
                assertLastLog("escaped={}", level.colotok)
            }
        }
    }

    @Test
    fun slf4j2_fluent_api_uses_same_formatting_contract() {
        runBlocking {
            val logger = LoggerFactory.getLogger("fluent-contract-test-2")

            logger.atInfo()
                .setMessage("fluent={}")
                .addArgument("A")
                .log()

            assertLastLog("fluent=A", LogLevel.INFO)
        }
    }

    private enum class BridgeLevel(val colotok: Level) {
        TRACE(LogLevel.TRACE),
        DEBUG(LogLevel.DEBUG),
        INFO(LogLevel.INFO),
        WARN(LogLevel.WARN),
        ERROR(LogLevel.ERROR),
    }

    private fun marker(): Marker = BasicMarkerFactory().getMarker("ignored")

    private fun testProvider(level: Level = LogLevel.TRACE): TestProvider =
        TestProvider(level).also(providersToClose::add)

    private fun logPlain(
        logger: Logger,
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
        logger: Logger,
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
        logger: Logger,
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
        logger: Logger,
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
        logger: Logger,
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
        logger: Logger,
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

    private suspend fun assertLastLog(
        expectedMessage: String,
        expectedLevel: Level,
        assertAttrs: (Map<String, String>) -> Unit = {}
    ) {
        provider.flush()
        assertEquals(expectedMessage, provider.lastMsg)
        assertEquals(expectedLevel, provider.lastLevel)
        assertAttrs(provider.lastAttr ?: emptyMap())
    }

    private fun assertFullStackTrace(actual: String, throwable: Throwable) {
        assertTrue(actual.contains(throwable::class.simpleName!!))
        assertTrue(actual.contains("at "))
        assertTrue(actual.contains("Caused by:"))
        assertTrue(actual.contains("IllegalStateException: root"))
    }
}
