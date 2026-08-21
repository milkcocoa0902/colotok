package com.milkcocoa.info.colotok.core.provider.builtin

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.builtin.stream.StreamProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class StreamProviderFailureTest {
    @Test
    fun formatter_failure_is_observable_from_flush_and_join() =
        runTest {
            val failure = IllegalStateException("format failed")
            val provider =
                StreamProvider {
                    formatter = ThrowingFormatter(failure)
                }

            provider.write(LogRecord.PlainText("test", "message", LogLevel.INFO, emptyMap()))

            assertSame(failure, assertFailsWith<IllegalStateException> { provider.flush() })
            assertSame(failure, assertFailsWith<IllegalStateException> { provider.join() })
        }

    private class ThrowingFormatter(
        private val failure: IllegalStateException
    ) : Formatter {
        override fun format(record: LogRecord.PlainText): String = throw failure

        override fun <T : LogStructure> format(record: LogRecord.StructuredText<T>): String = throw failure

        override fun format(record: LogRecord.Metrics): String = throw failure
    }
}