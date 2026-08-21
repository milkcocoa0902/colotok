package com.milkcocoa.info.colotok.core.provider.builtin.console

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ConsoleProviderPolicyTest {
    @Test
    fun default_configuration_disables_release_output() {
        val config = ConsoleProviderConfig()

        assertTrue(config.isOutputEnabled)
        assertFalse(config.isEnabledForRelease)
        assertFalse(isConsoleOutputEnabled(config.isOutputEnabled, config.isEnabledForRelease) { false })
    }

    @Test
    fun explicit_release_opt_in_enables_output() {
        assertTrue(isConsoleOutputEnabled(true, true) { false })
    }

    @Test
    fun debug_callback_enables_non_release_output() {
        assertTrue(isConsoleOutputEnabled(true, false) { true })
    }

    @Test
    fun explicit_disable_wins_over_release_and_debug_settings() {
        assertFalse(isConsoleOutputEnabled(false, true) { true })
    }

    @Test
    fun long_tag_is_truncated_on_android_25_and_older() {
        val name = "123456789012345678901234567890"

        assertEquals("12345678901234567890123", normalizeLogTag(name, 25))
        assertEquals("12345678901234567890123", normalizeLogTag(name, 24))
    }

    @Test
    fun long_tag_is_preserved_on_android_26_and_newer() {
        val name = "123456789012345678901234567890"

        assertEquals(name, normalizeLogTag(name, 26))
    }

    @Test
    fun formatter_failure_is_observable_through_flush_and_join() =
        runBlocking {
            val expected = IllegalStateException("format failed")
            val formatter =
                object : Formatter {
                    override fun format(record: LogRecord.PlainText): String = throw expected

                    override fun <T : LogStructure> format(record: LogRecord.StructuredText<T>): String = throw expected

                    override fun format(record: LogRecord.Metrics): String = throw expected
                }
            val provider =
                ConsoleProvider {
                    isEnabledForRelease = true
                    this.formatter = formatter
                }
            provider.write(LogRecord.PlainText("test", "message", LogLevel.INFO, emptyMap()))

            assertSame(expected, assertFailsWith<IllegalStateException> { provider.flush() })
            assertSame(expected, assertFailsWith<IllegalStateException> { provider.join() })
        }
}