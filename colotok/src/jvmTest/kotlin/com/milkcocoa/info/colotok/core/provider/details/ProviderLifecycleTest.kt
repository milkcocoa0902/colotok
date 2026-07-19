package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import java.util.concurrent.atomic.AtomicInteger

class ProviderLifecycleTest {
    private class RecordingProvider : Provider(ConsoleProviderConfig()) {
        val records = mutableListOf<LogRecord>()
        val flushCount = AtomicInteger(0)
        val closedCount = AtomicInteger(0)

        override suspend fun onMessage(record: LogRecord) {
            records.add(record)
        }

        override suspend fun onFlush() {
            flushCount.incrementAndGet()
        }

        override fun onClosed() {
            closedCount.incrementAndGet()
        }
    }

    @Test
    fun flush_waits_for_accepted_records_and_calls_onFlush() = runBlocking {
        val provider = RecordingProvider()
        val record: LogRecord = LogRecord.PlainText("test", "message", LogLevel.INFO, emptyMap())

        provider.write(record)
        provider.flush()

        assertEquals(listOf<LogRecord>(record), provider.records)
        assertTrue(provider.flushCount.get() >= 1)

        provider.join()
    }

    @Test
    fun flush_after_close_throws_provider_closed_exception() = runBlocking {
        val provider = RecordingProvider()

        provider.close()
        assertFailsWith<ProviderClosedException> { provider.flush() }
        provider.join()

        assertEquals(1, provider.closedCount.get())
    }

    @Test
    fun flush_after_force_throws_provider_closed_exception() = runBlocking {
        val provider = RecordingProvider()

        provider.forceShutdown()
        assertFailsWith<ProviderClosedException> { provider.flush() }
    }

    @Test
    fun join_after_force_does_not_report_graceful_success() = runBlocking {
        val provider = RecordingProvider()

        provider.forceShutdown()

        assertFailsWith<ProviderClosedException> { provider.join() }
    }

    @Test
    fun on_flush_failure_completes_pin_exceptionally() = runBlocking {
        val expected = IllegalStateException("flush failed")
        val provider = object : Provider(ConsoleProviderConfig()) {
            override suspend fun onMessage(record: LogRecord) = Unit

            override suspend fun onFlush() {
                throw expected
            }
        }

        val actual = assertFailsWith<IllegalStateException> { provider.flush() }

        assertTrue(actual === expected)
        assertTrue(assertFailsWith<IllegalStateException> { provider.join() } === expected)
    }

    @Test
    fun on_message_failure_is_rethrown_by_flush_and_join() = runBlocking {
        val expected = IllegalArgumentException("message failed")
        val provider = object : Provider(ConsoleProviderConfig()) {
            override suspend fun onMessage(record: LogRecord) {
                throw expected
            }
        }
        provider.write(LogRecord.PlainText("test", "message", LogLevel.INFO, emptyMap()))

        val flushFailure = assertFailsWith<IllegalArgumentException> { provider.flush() }

        assertTrue(flushFailure === expected)
        assertTrue(assertFailsWith<IllegalArgumentException> { provider.join() } === expected)
    }

    @Test
    fun on_closed_runs_once_for_repeated_shutdown_calls() = runBlocking {
        val provider = RecordingProvider()

        provider.close()
        provider.close()
        provider.join()
        provider.join()
        provider.forceShutdown()

        assertEquals(1, provider.closedCount.get())
    }

    @Test
    fun flush_excludes_records_accepted_after_its_marker() = runBlocking {
        val flushEntered = CompletableDeferred<Unit>()
        val releaseFlush = CompletableDeferred<Unit>()
        val secondEntered = CompletableDeferred<Unit>()
        val releaseSecond = CompletableDeferred<Unit>()
        val provider = object : Provider(ConsoleProviderConfig()) {
            val processed = mutableListOf<String>()

            override suspend fun onMessage(record: LogRecord) {
                val message = (record as LogRecord.PlainText).msg
                if (message == "second") {
                    secondEntered.complete(Unit)
                    releaseSecond.await()
                }
                processed += message
            }

            override suspend fun onFlush() {
                flushEntered.complete(Unit)
                releaseFlush.await()
            }
        }

        provider.write(LogRecord.PlainText("test", "first", LogLevel.INFO, emptyMap()))
        val flush = async { provider.flush() }
        flushEntered.await()
        provider.write(LogRecord.PlainText("test", "second", LogLevel.INFO, emptyMap()))
        releaseFlush.complete(Unit)

        flush.await()
        assertEquals(listOf("first"), provider.processed)

        secondEntered.await()
        releaseSecond.complete(Unit)
        provider.join()
        assertEquals(listOf("first", "second"), provider.processed)
    }

    @Test
    fun concurrent_flushes_complete_independently() = runBlocking {
        val provider = RecordingProvider()

        val first = async { provider.flush() }
        val second = async { provider.flush() }
        first.await()
        second.await()

        assertEquals(2, provider.flushCount.get())
        provider.join()
    }

    @Test
    fun flush_timeout_includes_waiting_to_enqueue_marker() = runBlocking {
        val messageEntered = CompletableDeferred<Unit>()
        val releaseMessage = CompletableDeferred<Unit>()
        val provider = object : Provider(ConsoleProviderConfig()) {
            override suspend fun onMessage(record: LogRecord) {
                messageEntered.complete(Unit)
                releaseMessage.await()
            }
        }
        val record = LogRecord.PlainText("test", "message", LogLevel.INFO, emptyMap())
        provider.write(record)
        messageEntered.await()
        while (provider.channel.trySend(record).isSuccess) {
            // Saturate the channel while the worker is held in onMessage().
        }

        assertFailsWith<TimeoutCancellationException> {
            provider.flush(50.milliseconds)
        }

        releaseMessage.complete(Unit)
        provider.forceShutdown()
    }

    @Test
    fun flush_marker_linearized_before_close_completes_successfully() = runBlocking {
        val flushEntered = CompletableDeferred<Unit>()
        val releaseFlush = CompletableDeferred<Unit>()
        val provider = object : Provider(ConsoleProviderConfig()) {
            override suspend fun onMessage(record: LogRecord) = Unit

            override suspend fun onFlush() {
                flushEntered.complete(Unit)
                releaseFlush.await()
            }
        }

        val flush = async { provider.flush() }
        flushEntered.await()
        provider.close()
        releaseFlush.complete(Unit)

        flush.await()
        provider.join()
    }

    @Test
    fun write_before_close_is_drained_and_write_after_close_is_rejected() = runBlocking {
        val provider = RecordingProvider()
        val accepted = LogRecord.PlainText("test", "accepted", LogLevel.INFO, emptyMap())
        val rejected = LogRecord.PlainText("test", "rejected", LogLevel.INFO, emptyMap())

        provider.write(accepted)
        provider.close()
        provider.write(rejected)
        provider.join()

        assertEquals(listOf<LogRecord>(accepted), provider.records)
    }

    @Test
    fun on_closed_failure_is_preserved_for_graceful_and_force_shutdown() = runBlocking {
        val gracefulFailure = IllegalStateException("graceful close failed")
        val graceful = object : Provider(ConsoleProviderConfig()) {
            override suspend fun onMessage(record: LogRecord) = Unit
            override fun onClosed() = throw gracefulFailure
        }
        graceful.close()
        assertTrue(assertFailsWith<IllegalStateException> { graceful.join() } === gracefulFailure)

        val forceFailure = IllegalArgumentException("force close failed")
        val forced = object : Provider(ConsoleProviderConfig()) {
            override suspend fun onMessage(record: LogRecord) = Unit
            override fun onClosed() = throw forceFailure
        }
        assertTrue(assertFailsWith<IllegalArgumentException> { forced.forceShutdown() } === forceFailure)
    }

    @Test
    fun graceful_final_flush_failure_makes_join_fail_with_original_cause() = runBlocking {
        val expected = IllegalStateException("final flush failed")
        val provider = object : Provider(ConsoleProviderConfig()) {
            override suspend fun onMessage(record: LogRecord) = Unit
            override suspend fun onFlush() = throw expected
        }

        provider.close()

        assertTrue(assertFailsWith<IllegalStateException> { provider.join() } === expected)
    }
}
