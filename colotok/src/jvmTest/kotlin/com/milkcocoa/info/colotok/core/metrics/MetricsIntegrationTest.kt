package com.milkcocoa.info.colotok.core.metrics

import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.ColotokLoggerContext
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetricsIntegrationTest {
    private companion object {
        const val SATURATION_ATTEMPT_LIMIT = 10_000
    }

    private val providersToClose = mutableListOf<Provider>()

    private fun <T : Provider> T.track(): T = also(providersToClose::add)

    @AfterTest
    fun tearDown() {
        providersToClose.forEach { provider ->
            runCatching { provider.forceShutdown() }
        }
        providersToClose.clear()
    }

    private class TestMetricsCollector : MetricsCollector {
        val logCounts = mutableMapOf<Pair<Level, String>, Int>()
        val errorCounts = mutableMapOf<Pair<String, String>, Int>()

        override fun incrementLogCount(level: Level, providerName: String) {
            val key = level to providerName
            logCounts[key] = logCounts.getOrDefault(key, 0) + 1
        }

        override fun incrementErrorCount(providerName: String, errorType: String) {
            val key = providerName to errorType
            errorCounts[key] = errorCounts.getOrDefault(key, 0) + 1
        }

        override fun updateBufferSize(providerName: String, size: Int) {}
        override fun recordWriteDuration(providerName: String, durationMs: Long) {}
    }

    private class StubProvider(config: ConsoleProviderConfig) : Provider(config) {
        override suspend fun onMessage(record: LogRecord) {}
    }

    private object ThrowingMetricsCollector : MetricsCollector {
        override fun incrementLogCount(level: Level, providerName: String) = error("metrics failed")
        override fun incrementErrorCount(providerName: String, errorType: String) = error("metrics failed")
        override fun updateBufferSize(providerName: String, size: Int) = error("metrics failed")
        override fun recordWriteDuration(providerName: String, durationMs: Long) = error("metrics failed")
    }

    @Test
    fun testMetricsInheritance() {
        val collector = TestMetricsCollector()
        val provider = StubProvider(ConsoleProviderConfig()).track()
        
        val context = ColotokLoggerContext()
            .withMetrics(collector)
            .addProvider(provider)
        
        val logger = context.getLogger()
        logger.info("test message")
        
        // Provider should have inherited the collector from context
        assertEquals(1, collector.logCounts.values.sum())
        assertTrue(collector.logCounts.containsKey(com.milkcocoa.info.colotok.core.level.LogLevel.INFO to "StubProvider"))
    }

    @Test
    fun testExplicitMetrics() {
        val contextCollector = TestMetricsCollector()
        val explicitCollector = TestMetricsCollector()
        
        val provider =
            StubProvider(ConsoleProviderConfig().apply {
                metricsSpec = MetricsCollectorSpec.Explicit(explicitCollector)
            }).track()
        
        val context = ColotokLoggerContext()
            .withMetrics(contextCollector)
            .addProvider(provider)
        
        val logger = context.getLogger()
        logger.info("test message")
        
        // Provider should use explicit collector, not the one from context
        assertEquals(0, contextCollector.logCounts.values.sum())
        assertEquals(1, explicitCollector.logCounts.values.sum())
    }

    @Test
    fun testNoOpMetrics() {
        val contextCollector = TestMetricsCollector()
        
        val provider =
            StubProvider(ConsoleProviderConfig().apply {
                metricsSpec = MetricsCollectorSpec.NoOp
            }).track()
        
        val context = ColotokLoggerContext()
            .withMetrics(contextCollector)
            .addProvider(provider)
        
        val logger = context.getLogger()
        logger.info("test message")
        
        // Provider should use NoOp collector
        assertEquals(0, contextCollector.logCounts.values.sum())
    }

    @Test
    fun testInternalLoggingMetrics() = runBlocking {
        val receivedRecords = mutableListOf<LogRecord>()
        class RecordingProvider(config: ConsoleProviderConfig) : Provider(config) {
            override suspend fun onMessage(record: LogRecord) {
                receivedRecords.add(record)
            }
        }

        val provider =
            RecordingProvider(ConsoleProviderConfig().apply {
                enableInternalMetricsLogging = true
            }).track()

        val context = ColotokLoggerContext()
            .addProvider(provider)

        val logger = context.getLogger()
        logger.info("test message")

        // Wait for processing
        provider.flush()

        // Should have "test message" and the metrics log record
        assertTrue(receivedRecords.any { it is LogRecord.PlainText && it.msg == "test message" })
        assertTrue(receivedRecords.any { it is LogRecord.Metrics && it.msg.contains("LogCount increased") })

        // Check that LogRecord.Metrics itself did not trigger another LogRecord.Metrics
        // (If it did, we'd have many LogRecord.Metrics or an infinite loop if not guarded)
        val metricsRecordsCount = receivedRecords.count { it is LogRecord.Metrics }
        assertEquals(1, metricsRecordsCount, "Should only have 1 metrics record for the initial log")
    }

    @Test
    fun testMetricsBothInheritAndExplicit() {
        val contextCollector = TestMetricsCollector()
        val explicitCollector = TestMetricsCollector()

        val provider =
            StubProvider(ConsoleProviderConfig().apply {
                metricsSpec = MetricsCollectorSpec.Explicit(explicitCollector, inheritParent = true)
            }).track()

        val context = ColotokLoggerContext()
            .withMetrics(contextCollector)
            .addProvider(provider)

        val logger = context.getLogger()
        logger.info("test message")

        // Provider should use both collectors
        assertEquals(1, contextCollector.logCounts.values.sum(), "Inherited collector should be called")
        assertEquals(1, explicitCollector.logCounts.values.sum(), "Explicit collector should be called")
        assertTrue(contextCollector.logCounts.containsKey(LogLevel.INFO to "StubProvider"))
        assertTrue(explicitCollector.logCounts.containsKey(LogLevel.INFO to "StubProvider"))
    }

    @Test
    fun testMetricsInternalAndInherit() = runBlocking {
        val contextCollector = TestMetricsCollector()
        val receivedRecords = mutableListOf<LogRecord>()
        class RecordingProvider(config: ConsoleProviderConfig) : Provider(config) {
            override suspend fun onMessage(record: LogRecord) {
                receivedRecords.add(record)
            }
        }

        val provider =
            RecordingProvider(ConsoleProviderConfig().apply {
                metricsSpec = MetricsCollectorSpec.Inherit
                enableInternalMetricsLogging = true
            }).track()

        val context = ColotokLoggerContext()
            .withMetrics(contextCollector)
            .addProvider(provider)

        val logger = context.getLogger()
        logger.info("test message")

        provider.flush()

        // Should have incremented the context collector
        assertEquals(1, contextCollector.logCounts.values.sum(), "Inherited collector should be called even with internal logging")

        // Should also have the metrics log record
        assertTrue(receivedRecords.any { it is LogRecord.Metrics && it.msg.contains("LogCount increased") }, "Internal logging should be performed")
    }

    @Test
    fun internal_logging_of_buffer_rejection_is_finite_and_non_recursive() = runBlocking {
        val collector = TestMetricsCollector()
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val receivedRecords = mutableListOf<LogRecord>()
        val provider = object : Provider(ConsoleProviderConfig().apply {
            enableInternalMetricsLogging = true
        }) {
            override suspend fun onMessage(record: LogRecord) {
                if (record is LogRecord.PlainText && record.msg == "blocking") {
                    firstEntered.complete(Unit)
                    releaseFirst.await()
                }
                receivedRecords += record
            }
        }.track()
        val logger = ColotokLoggerContext()
            .withMetrics(collector)
            .addProvider(provider)
            .getLogger()

        try {
            logger.info("blocking")
            firstEntered.await()

            var nextIndex = 0
            while (
                collector.errorCounts.values.sum() == 0 &&
                nextIndex < SATURATION_ATTEMPT_LIMIT
            ) {
                logger.info("queued-${nextIndex++}")
            }

            assertTrue(collector.errorCounts.isNotEmpty(), "queue did not reject within the finite attempt limit")
            assertEquals(1, collector.errorCounts.values.sum())
            assertTrue(collector.errorCounts.keys.any { it.second == "buffer_full" })

            releaseFirst.complete(Unit)
            provider.join()

            assertEquals(1, collector.errorCounts.values.sum())
            assertTrue(receivedRecords.isNotEmpty())
        } finally {
            releaseFirst.complete(Unit)
            runCatching { provider.forceShutdown() }
        }
    }

    @Test
    fun collector_failure_does_not_fail_write_flush_or_join() = runBlocking {
        val provider = StubProvider(ConsoleProviderConfig()).apply {
            effectiveMetricsCollector = ThrowingMetricsCollector
        }
        val record = LogRecord.PlainText("test", "message", LogLevel.INFO, emptyMap())

        provider.write(record)
        provider.flush()
        provider.join()
    }

    @Test
    fun failing_composite_collector_does_not_skip_remaining_collectors() {
        val recordingCollector = TestMetricsCollector()
        val provider = StubProvider(ConsoleProviderConfig()).apply {
            effectiveMetricsCollector = CompositeMetricsCollector(
                listOf(ThrowingMetricsCollector, recordingCollector)
            )
        }

        provider.write(LogRecord.PlainText("test", "message", LogLevel.INFO, emptyMap()))

        assertEquals(1, recordingCollector.logCounts.values.sum())
        provider.forceShutdown()
    }
}
