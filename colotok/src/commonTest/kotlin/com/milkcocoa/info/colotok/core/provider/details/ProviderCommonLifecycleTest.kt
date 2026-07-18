package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.metrics.MetricsCollector
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ProviderCommonLifecycleTest {
    private val providersToClose = mutableListOf<Provider>()

    @AfterTest
    fun closeProviders() = runTest {
        providersToClose.forEach {
            runCatching { it.forceShutdown() }
            it.job.join()
        }
        providersToClose.clear()
    }

    private fun <T : Provider> T.track(): T = also(providersToClose::add)

    private suspend fun Provider.forceShutdownAndJoin() {
        try {
            forceShutdown()
        } finally {
            job.join()
        }
    }

    private class RecordingProvider(
        private val messageFailure: Throwable? = null,
        private val flushFailure: Throwable? = null,
    ) : Provider(ConsoleProviderConfig()) {
        val records = mutableListOf<LogRecord>()
        val lifecycleEvents = mutableListOf<String>()
        var flushCount = 0
        var closeCount = 0

        override suspend fun onMessage(record: LogRecord) {
            messageFailure?.let { throw it }
            records.add(record)
            lifecycleEvents.add("message")
        }

        override suspend fun onFlush() {
            flushCount++
            lifecycleEvents.add("flush")
            flushFailure?.let { throw it }
        }

        override fun onClosed() {
            closeCount++
            lifecycleEvents.add("closed")
        }
    }

    private object ThrowingMetricsCollector : MetricsCollector {
        override fun incrementLogCount(level: Level, providerName: String) = error("metrics failed")
        override fun incrementErrorCount(providerName: String, errorType: String) = error("metrics failed")
        override fun updateBufferSize(providerName: String, size: Int) = error("metrics failed")
        override fun recordWriteDuration(providerName: String, durationMs: Long) = error("metrics failed")
    }

    private fun record(message: String = "message"): LogRecord =
        LogRecord.PlainText("test", message, LogLevel.INFO, emptyMap())

    @Test
    fun write_then_flush_processes_the_record_and_invokes_on_flush() = runTest {
        val provider = RecordingProvider().track()
        val expected = record()

        try {
            provider.write(expected)
            provider.flush()

            assertEquals(listOf(expected), provider.records)
            assertEquals(1, provider.flushCount)
        } finally {
            provider.forceShutdownAndJoin()
        }
    }

    @Test
    fun flush_after_close_throws_provider_closed_exception() = runTest {
        val provider = RecordingProvider().track()

        provider.close()
        try {
            assertFailsWith<ProviderClosedException> { provider.flush() }
        } finally {
            provider.join()
        }

        assertEquals(1, provider.closeCount)
    }

    @Test
    fun flush_after_force_shutdown_throws_provider_closed_exception() = runTest {
        val provider = RecordingProvider().track()

        provider.forceShutdownAndJoin()

        assertFailsWith<ProviderClosedException> { provider.flush() }
        assertEquals(1, provider.closeCount)
    }

    @Test
    fun graceful_shutdown_invokes_the_final_flush_before_closing() = runTest {
        val provider = RecordingProvider().track()

        provider.write(record())
        provider.close()
        provider.join()

        assertEquals(1, provider.flushCount)
        assertEquals(1, provider.closeCount)
        assertEquals(listOf("message", "flush", "closed"), provider.lifecycleEvents)
    }

    @Test
    fun on_message_failure_is_rethrown_by_flush_and_join_with_the_original_cause() = runTest {
        val expected = IllegalArgumentException("message failed")
        val provider = RecordingProvider(messageFailure = expected).track()
        provider.write(record())

        val flushFailure = assertFailsWith<IllegalArgumentException> { provider.flush() }
        val joinFailure = assertFailsWith<IllegalArgumentException> { provider.join() }

        assertSame(expected, flushFailure)
        assertSame(expected, joinFailure)
        assertEquals(1, provider.closeCount)
    }

    @Test
    fun on_flush_failure_is_rethrown_by_flush_and_join_with_the_original_cause() = runTest {
        val expected = IllegalStateException("flush failed")
        val provider = RecordingProvider(flushFailure = expected).track()

        val flushFailure = assertFailsWith<IllegalStateException> { provider.flush() }
        val joinFailure = assertFailsWith<IllegalStateException> { provider.join() }

        assertSame(expected, flushFailure)
        assertSame(expected, joinFailure)
        assertEquals(1, provider.closeCount)
    }

    @Test
    fun collector_failure_does_not_interrupt_write_flush_or_join() = runTest {
        val provider = RecordingProvider().apply {
            effectiveMetricsCollector = ThrowingMetricsCollector
        }.track()
        val expected = record()

        try {
            provider.write(expected)
            provider.flush()
        } finally {
            provider.close()
            provider.join()
        }

        assertEquals(listOf(expected), provider.records)
        assertEquals(2, provider.flushCount)
        assertEquals(1, provider.closeCount)
    }
}
