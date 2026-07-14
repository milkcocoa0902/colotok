package com.milkcocoa.info.colotok.core.provider.builtin.console

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.test.Test
import kotlin.test.assertSame

class ConsoleProviderFailureJsTest {
    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun formatter_failure_is_observable_from_flush_and_join() = GlobalScope.promise {
        val failure = IllegalStateException("format failed")
        val config = ConsoleProviderConfig().apply {
            formatter = ThrowingFormatter(failure)
        }
        val provider = ConsoleProvider(config)
        provider.write(LogRecord.PlainText("test", "message", LogLevel.INFO, emptyMap()))

        val flushFailure = try {
            provider.flush()
            null
        } catch (throwable: Throwable) {
            throwable
        }
        val joinFailure = try {
            provider.join()
            null
        } catch (throwable: Throwable) {
            throwable
        }

        assertSame(failure, flushFailure)
        assertSame(failure, joinFailure)
    }

    private class ThrowingFormatter(
        private val failure: IllegalStateException,
    ) : Formatter {
        override fun format(record: LogRecord.PlainText): String = throw failure

        override fun <T : LogStructure> format(record: LogRecord.StructuredText<T>): String = throw failure

        override fun format(record: LogRecord.Metrics): String = throw failure
    }
}
