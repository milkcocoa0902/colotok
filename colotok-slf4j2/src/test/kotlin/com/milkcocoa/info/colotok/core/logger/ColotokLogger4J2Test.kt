package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.serialization.KSerializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class ColotokLogger4J2Test {

    private class TestProvider : Provider {
        var lastName: String? = null
        var lastMsg: String? = null
        var lastLevel: Level? = null
        var lastAttr: Map<String, String>? = null

        override fun write(name: String, msg: String, level: Level, attr: Map<String, String>) {
            lastName = name
            lastMsg = msg
            lastLevel = level
            lastAttr = attr
        }

        override fun <T : LogStructure> write(
            name: String,
            msg: T,
            serializer: KSerializer<T>,
            level: Level,
            attr: Map<String, String>
        ) {
            lastName = name
            lastMsg = msg.toString()
            lastLevel = level
            lastAttr = attr
        }
    }

    private lateinit var provider: TestProvider
    private lateinit var originalDefault: ColotokLoggerContext

    @BeforeEach
    fun setup() {
        originalDefault = ColotokLoggerContext.DEFAULT
        provider = TestProvider()
        val ctx = ColotokLoggerContext()
            .addProvider(provider)
            .withAttrs(mapOf("base" to "attr"))
        ColotokLoggerContext.setDefault(ctx)
    }

    @AfterEach
    fun tearDown() {
        ColotokLoggerContext.setDefault(originalDefault)
    }

    @Test
    fun info_logs_are_delegated_with_default_attrs_and_logger_name() {
        val logger = LoggerFactory.getLogger("slf4j2-test")

        logger.info("hello world")

        assertEquals("slf4j2-test", provider.lastName)
        assertEquals("hello world", provider.lastMsg)
        assertEquals(LogLevel.INFO, provider.lastLevel)
        val attrs = provider.lastAttr ?: emptyMap()
        assertEquals("attr", attrs["base"])
        assertEquals("slf4j2-test", attrs["logger"])
    }

    @Test
    fun format_overloads_work_with_slf4j_placeholders() {
        val logger = LoggerFactory.getLogger("format-test-2")

        // SLF4J は {} プレースホルダを使用する
        logger.debug("value={}", "A")

        assertEquals(LogLevel.DEBUG, provider.lastLevel)
        assertEquals("value=A", provider.lastMsg)
        assertEquals("format-test-2", provider.lastName)
        assertEquals("format-test-2", provider.lastAttr?.get("logger"))
    }

    @Test
    fun throwable_is_added_as_attribute() {
        val logger = LoggerFactory.getLogger("throwable-test-2")
        val ex = IllegalArgumentException("boom")

        logger.error("oops", ex)

        assertEquals(LogLevel.ERROR, provider.lastLevel)
        assertEquals("oops", provider.lastMsg)
        val attrs = provider.lastAttr ?: emptyMap()
        assertTrue(attrs.containsKey("cause"))
        assertTrue(attrs["cause"]!!.contains("IllegalArgumentException"))
        assertEquals("throwable-test-2", attrs["logger"])
    }
}
