package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun flush_after_close_is_silent() = runBlocking {
        val provider = RecordingProvider()

        provider.close()
        provider.flush()
        provider.join()

        assertEquals(1, provider.closedCount.get())
    }

    @Test
    fun flush_after_forceShutdown_is_silent() = runBlocking {
        val provider = RecordingProvider()

        provider.forceShutdown()
        provider.flush()
    }
}
