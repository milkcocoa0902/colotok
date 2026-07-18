package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.formatter.builtin.text.SimpleTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.metrics.MetricsCollectorSpec
import com.milkcocoa.info.colotok.core.metrics.MetricsCollector
import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AsyncProviderTest {

    private val providersToClose = mutableListOf<AsyncProvider>()

    @AfterTest
    fun closeProviders() {
        providersToClose.forEach { runCatching { it.forceShutdown() } }
        providersToClose.clear()
    }

    private fun <T : AsyncProvider> T.track(): T = also(providersToClose::add)

    class TestAsyncProviderConfig : AsyncProviderConfig {
        override var level: Level = LogLevel.DEBUG
        override var formatter: Formatter = SimpleTextFormatter
        override var bufferSize: Int = 1
        override var metricsSpec: MetricsCollectorSpec = MetricsCollectorSpec.Inherit
        override var enableInternalMetricsLogging: Boolean = false
    }

    class TestAsyncProvider(config: TestAsyncProviderConfig) : AsyncProvider(config) {
        val processedRecords = mutableListOf<LogRecord>()

        override suspend fun onPublish(records: List<LogRecord>) {
            processedRecords.addAll(records)
        }
    }

    class FailingOnceAsyncProvider(config: TestAsyncProviderConfig) : AsyncProvider(config) {
        val publishAttempts = mutableListOf<List<LogRecord>>()
        val processedRecords = mutableListOf<LogRecord>()
        var shouldFail = true

        override suspend fun onPublish(records: List<LogRecord>) {
            publishAttempts.add(records)
            if (shouldFail) {
                shouldFail = false
                error("publish failed")
            }
            processedRecords.addAll(records)
        }
    }

    class ControlledAsyncProvider(
        config: TestAsyncProviderConfig,
        private var failuresRemaining: Int = Int.MAX_VALUE,
    ) : AsyncProvider(config) {
        val publishAttempts = mutableListOf<List<LogRecord>>()
        val processedRecords = mutableListOf<LogRecord>()

        override suspend fun onPublish(records: List<LogRecord>) {
            publishAttempts.add(records)
            if (failuresRemaining > 0) {
                failuresRemaining--
                error("publish failed")
            }
            processedRecords.addAll(records)
        }
    }

    class RecordingMetricsCollector : MetricsCollector {
        val errors = mutableListOf<String>()
        val bufferSizes = mutableListOf<Int>()
        var logCount = 0
        var durations = 0

        override fun incrementLogCount(level: Level, providerName: String) {
            logCount++
        }

        override fun incrementErrorCount(providerName: String, errorType: String) {
            errors.add(errorType)
        }

        override fun updateBufferSize(providerName: String, size: Int) {
            bufferSizes.add(size)
        }

        override fun recordWriteDuration(providerName: String, durationMs: Long) {
            durations++
        }
    }

    private object ThrowingMetricsCollector : MetricsCollector {
        override fun incrementLogCount(level: Level, providerName: String) = error("metrics failed")
        override fun incrementErrorCount(providerName: String, errorType: String) = error("metrics failed")
        override fun updateBufferSize(providerName: String, size: Int) = error("metrics failed")
        override fun recordWriteDuration(providerName: String, durationMs: Long) = error("metrics failed")
    }

    private fun record(index: Int): LogRecord =
        LogRecord.PlainText("test", "message $index", LogLevel.INFO, emptyMap())

    @Test
    fun testAsyncLogging() = runTest {
        val provider = TestAsyncProvider(TestAsyncProviderConfig()).track()
        val record1: LogRecord = LogRecord.PlainText("test", "message 1", LogLevel.INFO, emptyMap())
        val record2: LogRecord = LogRecord.PlainText("test", "message 2", LogLevel.INFO, emptyMap())

        provider.write(record1)
        provider.write(record2)

        // Wait for processing to complete
        provider.flush()

        assertEquals(2, provider.processedRecords.size)
        assertEquals(record1, provider.processedRecords[0])
        assertEquals(record2, provider.processedRecords[1])

        provider.join()
    }

    @Test
    fun failedPublishRecordsAreRetainedForNextSendTrigger() = runTest {
        val provider = FailingOnceAsyncProvider(TestAsyncProviderConfig()).track()
        val record1: LogRecord = LogRecord.PlainText("test", "message 1", LogLevel.INFO, emptyMap())
        val record2: LogRecord = LogRecord.PlainText("test", "message 2", LogLevel.INFO, emptyMap())

        provider.write(record1)
        provider.write(record2)
        provider.flush()

        assertEquals(listOf<LogRecord>(record1), provider.publishAttempts[0])
        assertEquals(listOf<LogRecord>(record1, record2), provider.publishAttempts[1])
        assertEquals(listOf<LogRecord>(record1, record2), provider.processedRecords)
        assertTrue(provider.publishAttempts.size >= 2)

        provider.join()
    }

    @Test
    fun failed_backlog_retries_only_at_threshold_multiples() = runTest {
        val config = TestAsyncProviderConfig().apply { bufferSize = 2 }
        val provider = ControlledAsyncProvider(config, failuresRemaining = 2).track()

        (1..6).forEach { provider.write(record(it)) }
        provider.flush()

        assertEquals(listOf(2, 4, 6), provider.publishAttempts.map { it.size })
        assertEquals(
            (1..6).map { "message $it" },
            provider.processedRecords.map { (it as LogRecord.PlainText).msg },
        )
        provider.join()
    }

    @Test
    fun failed_backlog_can_expand_to_four_times_buffer_size_and_keeps_fifo() = runTest {
        val config = TestAsyncProviderConfig().apply { bufferSize = 2 }
        val provider = ControlledAsyncProvider(config).track()

        (1..8).forEach { provider.write(record(it)) }
        assertFailsWith<IllegalStateException> { provider.flush() }

        assertEquals(listOf(2, 4, 6, 8, 8), provider.publishAttempts.map { it.size })
        assertEquals(
            (1..8).map { "message $it" },
            provider.publishAttempts.last().map { (it as LogRecord.PlainText).msg },
        )
        runCatching { provider.forceShutdown() }
    }

    @Test
    fun newest_record_is_dropped_after_retention_capacity() = runTest {
        val config = TestAsyncProviderConfig().apply { bufferSize = 1 }
        val metrics = RecordingMetricsCollector()
        val provider = ControlledAsyncProvider(config).apply {
            effectiveMetricsCollector = metrics
        }.track()

        (1..5).forEach { provider.write(record(it)) }
        assertFailsWith<IllegalStateException> { provider.flush() }

        assertEquals(listOf(1, 2, 3, 4, 4), provider.publishAttempts.map { it.size })
        assertEquals(
            (1..4).map { "message $it" },
            provider.publishAttempts.last().map { (it as LogRecord.PlainText).msg },
        )
        assertEquals(1, metrics.errors.count { it == "retention_limit_reached" })
        assertEquals(4, metrics.bufferSizes.last())
        runCatching { provider.forceShutdown() }
    }

    @Test
    fun retention_capacity_is_capped_at_4096_records() = runTest {
        val config = TestAsyncProviderConfig().apply { bufferSize = 2_000 }
        val metrics = RecordingMetricsCollector()
        val provider = ControlledAsyncProvider(config).apply {
            effectiveMetricsCollector = metrics
        }.track()

        (1..4_097).forEach { provider.writeAsync(record(it)) }
        assertFailsWith<IllegalStateException> { provider.flush() }

        assertEquals(listOf(2_000, 4_000, 4_096, 4_096), provider.publishAttempts.map { it.size })
        assertEquals(1, metrics.errors.count { it == "retention_limit_reached" })
        runCatching { provider.forceShutdown() }
    }

    @Test
    fun manual_flush_failure_retains_batch_and_throws() = runTest {
        val config = TestAsyncProviderConfig().apply { bufferSize = 4 }
        val provider = ControlledAsyncProvider(config).track()
        provider.write(record(1))

        val failure = assertFailsWith<IllegalStateException> { provider.flush() }

        assertEquals("publish failed", failure.message)
        assertEquals(
            listOf("message 1"),
            provider.publishAttempts.single().map { (it as LogRecord.PlainText).msg },
        )
        runCatching { provider.forceShutdown() }
    }

    @Test
    fun invalid_buffer_size_fails_before_worker_start() {
        listOf(-1, 0, 4_097).forEach { invalidSize ->
            assertFailsWith<IllegalArgumentException> {
                TestAsyncProvider(TestAsyncProviderConfig().apply { bufferSize = invalidSize })
            }
        }
    }

    @Test
    fun metrics_record_does_not_generate_metrics() = runTest {
        val metrics = RecordingMetricsCollector()
        val provider = TestAsyncProvider(TestAsyncProviderConfig()).apply {
            effectiveMetricsCollector = metrics
        }.track()
        val metricsRecord = LogRecord.Metrics("metrics", "value", LogLevel.INFO, emptyMap())

        provider.write(metricsRecord)
        provider.flush()

        assertEquals(0, metrics.logCount)
        assertEquals(emptyList(), metrics.errors)
        assertEquals(emptyList(), metrics.bufferSizes)
        assertEquals(0, metrics.durations)
        provider.join()
    }

    @Test
    fun metrics_record_publish_failure_does_not_generate_error_metric() = runTest {
        val metrics = RecordingMetricsCollector()
        val provider = ControlledAsyncProvider(TestAsyncProviderConfig()).apply {
            effectiveMetricsCollector = metrics
        }.track()

        provider.write(LogRecord.Metrics("metrics", "value", LogLevel.INFO, emptyMap()))
        assertFailsWith<IllegalStateException> { provider.flush() }

        assertEquals(0, metrics.logCount)
        assertEquals(emptyList(), metrics.errors)
        assertEquals(emptyList(), metrics.bufferSizes)
        assertEquals(0, metrics.durations)
        runCatching { provider.forceShutdown() }
    }

    @Test
    fun mixed_batch_publish_failure_reports_normal_record_metrics() = runTest {
        val metrics = RecordingMetricsCollector()
        val provider = ControlledAsyncProvider(
            TestAsyncProviderConfig().apply { bufferSize = 2 }
        ).apply {
            effectiveMetricsCollector = metrics
        }.track()

        provider.write(record(1))
        provider.write(LogRecord.Metrics("metrics", "value", LogLevel.INFO, emptyMap()))
        assertFailsWith<IllegalStateException> { provider.flush() }

        assertEquals(1, metrics.logCount)
        assertEquals(2, metrics.errors.count { it == "publish_failed" })
        assertEquals(listOf(1), metrics.bufferSizes)
        runCatching { provider.forceShutdown() }
    }

    @Test
    fun mixed_batch_publish_success_reports_cleared_buffer() = runTest {
        val metrics = RecordingMetricsCollector()
        val provider = ControlledAsyncProvider(
            TestAsyncProviderConfig().apply { bufferSize = 2 },
            failuresRemaining = 0,
        ).apply {
            effectiveMetricsCollector = metrics
        }.track()

        provider.write(record(1))
        provider.write(LogRecord.Metrics("metrics", "value", LogLevel.INFO, emptyMap()))
        provider.flush()

        assertEquals(1, metrics.logCount)
        assertEquals(emptyList(), metrics.errors)
        assertEquals(listOf(1, 0), metrics.bufferSizes)
        provider.join()
    }

    @Test
    fun dropped_metrics_record_does_not_report_retention_error() = runTest {
        val metrics = RecordingMetricsCollector()
        val provider = ControlledAsyncProvider(TestAsyncProviderConfig()).apply {
            effectiveMetricsCollector = metrics
        }.track()

        (1..4).forEach { provider.write(record(it)) }
        provider.write(LogRecord.Metrics("metrics", "value", LogLevel.INFO, emptyMap()))
        assertFailsWith<IllegalStateException> { provider.flush() }

        assertEquals(0, metrics.errors.count { it == "retention_limit_reached" })
        runCatching { provider.forceShutdown() }
    }

    @Test
    fun collector_failure_does_not_break_async_worker_or_flush() = runTest {
        val provider = TestAsyncProvider(TestAsyncProviderConfig()).apply {
            effectiveMetricsCollector = ThrowingMetricsCollector
        }.track()

        provider.write(record(1))
        provider.flush()

        assertEquals(listOf("message 1"), provider.processedRecords.map { (it as LogRecord.PlainText).msg })
        provider.join()
    }

    @Test
    fun write_async_reports_acceptance_metrics_without_metrics_recursion() = runTest {
        val metrics = RecordingMetricsCollector()
        val provider = TestAsyncProvider(TestAsyncProviderConfig().apply { bufferSize = 2 }).apply {
            effectiveMetricsCollector = metrics
        }.track()

        provider.writeAsync(record(1))
        provider.writeAsync(LogRecord.Metrics("metrics", "value", LogLevel.INFO, emptyMap()))
        provider.flush()

        assertEquals(1, metrics.logCount)
        assertEquals(emptyList(), metrics.errors)
        provider.join()
    }

    @Test
    fun write_async_after_close_is_rejected_and_reported() = runTest {
        val metrics = RecordingMetricsCollector()
        val provider = TestAsyncProvider(TestAsyncProviderConfig()).apply {
            effectiveMetricsCollector = metrics
        }.track()
        provider.close()

        provider.writeAsync(record(1))
        provider.join()

        assertEquals(0, metrics.logCount)
        assertEquals(listOf("buffer_full"), metrics.errors)
    }
}
