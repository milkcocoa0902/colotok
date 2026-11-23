package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.metrics.MetricsCollector
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds

class ProviderCommonLifecycleTest {
    private val providersToClose = mutableListOf<Provider>()

    @AfterTest
    fun closeProviders() =
        runTest {
            providersToClose.forEach {
                runCatching { it.forceShutdown() }
                runCatching { it.join() }
            }
            providersToClose.clear()
        }

    private fun <T : Provider> T.track(): T = also(providersToClose::add)

    private suspend fun Provider.forceShutdownAndJoin() {
        try {
            forceShutdown()
        } finally {
            assertFailsWith<ProviderClosedException> { join() }
        }
    }

    private class RecordingProvider(
        private val messageFailure: Throwable? = null,
        private val flushAction: suspend (Int) -> Unit = {}
    ) : Provider(ConsoleProviderConfig().apply { autoFlash = ProviderConfig.AutoFlash.Disabled }) {
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
            flushAction(flushCount)
        }

        override fun onClosed() {
            closeCount++
            lifecycleEvents.add("closed")
        }
    }

    private object ThrowingMetricsCollector : MetricsCollector {
        override fun incrementLogCount(
            level: Level,
            providerName: String
        ) = error("metrics failed")

        override fun incrementErrorCount(
            providerName: String,
            errorType: String
        ) = error("metrics failed")

        override fun updateBufferSize(
            providerName: String,
            size: Int
        ) = error("metrics failed")

        override fun recordWriteDuration(
            providerName: String,
            durationMs: Long
        ) = error("metrics failed")
    }

    private fun record(message: String = "message"): LogRecord =
        LogRecord.PlainText("test", message, LogLevel.INFO, emptyMap())

    @Test
    fun write_then_flush_processes_the_record_and_invokes_on_flush() =
        runTest {
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
    fun flush_after_close_throws_provider_closed_exception() =
        runTest {
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
    fun flush_after_force_shutdown_throws_provider_closed_exception() =
        runTest {
            val provider = RecordingProvider().track()

            provider.forceShutdownAndJoin()

            assertFailsWith<ProviderClosedException> { provider.flush() }
            assertEquals(1, provider.closeCount)
        }

    @Test
    fun graceful_shutdown_invokes_the_final_flush_before_closing() =
        runTest {
            val provider = RecordingProvider().track()

            provider.write(record())
            provider.close()
            provider.join()

            assertEquals(1, provider.flushCount)
            assertEquals(1, provider.closeCount)
            assertEquals(listOf("message", "flush", "closed"), provider.lifecycleEvents)
        }

    @Test
    fun on_message_failure_is_rethrown_by_flush_and_join_with_the_original_cause() =
        runTest {
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
    fun disabled_periodic_flush_preserves_explicit_and_final_flushes() =
        runTest {
            val flushed = CompletableDeferred<Unit>()
            val provider = RecordingProvider { flushed.complete(Unit) }.track()

            val periodicFlush = withContext(Dispatchers.Default) {
                withTimeoutOrNull(100.milliseconds) { flushed.await() }
            }
            assertNull(periodicFlush)

            provider.flush()
            provider.join()

            assertEquals(listOf("flush", "flush", "closed"), provider.lifecycleEvents)
        }

    @Test
    fun explicit_flush_failures_leave_queued_records_and_later_flushes_processable() =
        runTest {
            val expected = IllegalStateException("flush failed")
            val enteredFlush = CompletableDeferred<Unit>()
            val releaseFlush = CompletableDeferred<Unit>()
            val provider =
                RecordingProvider { attempt ->
                    if (attempt == 1) {
                        enteredFlush.complete(Unit)
                        releaseFlush.await()
                    }
                    if (attempt <= 2) throw expected
                }.track()
            val queued = record("queued during flush")
            val later = record("written after flush failure")

            val firstFlush = async { runCatching { provider.flush() } }
            enteredFlush.await()
            provider.write(queued)
            releaseFlush.complete(Unit)
            assertSame(expected, firstFlush.await().exceptionOrNull())
            assertSame(expected, assertFailsWith<IllegalStateException> { provider.flush() })

            provider.write(later)
            provider.flush()
            provider.join()

            assertEquals(listOf(queued, later), provider.records)
            assertEquals(
                listOf("flush", "message", "flush", "message", "flush", "flush", "closed"),
                provider.lifecycleEvents
            )
            assertEquals(1, provider.closeCount)
        }

    @Test
    fun timeout_inside_on_flush_is_reported_without_cancelling_the_worker() =
        runTest {
            val provider =
                RecordingProvider { attempt ->
                    if (attempt == 1) withTimeout(10) { awaitCancellation() }
                }.track()

            assertFailsWith<TimeoutCancellationException> { provider.flush() }
            provider.flush()
            provider.join()

            assertEquals(3, provider.flushCount)
            assertEquals(1, provider.closeCount)
        }

    @Test
    fun caller_timeout_does_not_cancel_an_in_flight_flush() =
        runTest {
            val enteredFlush = CompletableDeferred<Unit>()
            val releaseFlush = CompletableDeferred<Unit>()
            val provider =
                RecordingProvider { attempt ->
                    if (attempt == 1) {
                        enteredFlush.complete(Unit)
                        releaseFlush.await()
                    }
                }.track()
            val queued = record()

            try {
                val flush = async { runCatching { provider.flush() } }
                enteredFlush.await()
                provider.write(queued)
                assertIs<TimeoutCancellationException>(flush.await().exceptionOrNull())
            } finally {
                releaseFlush.complete(Unit)
            }

            provider.flush()
            provider.join()
            assertEquals(listOf("flush", "message", "flush", "flush", "closed"), provider.lifecycleEvents)
        }

    @Test
    fun later_message_failure_is_reported_by_join_independently_of_explicit_flush_failure() =
        runTest {
            val flushFailure = IllegalStateException("flush failed")
            val messageFailure = IllegalArgumentException("message failed")
            val enteredFlush = CompletableDeferred<Unit>()
            val releaseFlush = CompletableDeferred<Unit>()
            val provider =
                RecordingProvider(messageFailure = messageFailure) {
                    enteredFlush.complete(Unit)
                    releaseFlush.await()
                    throw flushFailure
                }.track()

            val flush = async { runCatching { provider.flush() } }
            enteredFlush.await()
            provider.write(record())
            releaseFlush.complete(Unit)

            assertSame(messageFailure, assertFailsWith<IllegalArgumentException> { provider.join() })
            assertSame(flushFailure, flush.await().exceptionOrNull())
        }

    @Test
    fun force_shutdown_cancels_the_worker_during_an_explicit_flush() =
        runTest {
            val enteredFlush = CompletableDeferred<Unit>()
            val provider =
                RecordingProvider {
                    enteredFlush.complete(Unit)
                    awaitCancellation()
                }.track()

            val flush = async { runCatching { provider.flush() } }
            enteredFlush.await()
            provider.forceShutdownAndJoin()

            assertIs<CancellationException>(flush.await().exceptionOrNull())
            assertEquals(listOf("flush", "closed"), provider.lifecycleEvents)
        }

    @Test
    fun collector_failure_does_not_interrupt_write_flush_or_join() =
        runTest {
            val provider =
                RecordingProvider().apply {
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
