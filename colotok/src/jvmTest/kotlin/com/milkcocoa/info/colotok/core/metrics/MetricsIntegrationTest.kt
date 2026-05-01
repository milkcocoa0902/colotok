package com.milkcocoa.info.colotok.core.metrics

import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.ColotokLoggerContext
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetricsIntegrationTest {

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

    @Test
    fun testMetricsInheritance() {
        val collector = TestMetricsCollector()
        val provider = StubProvider(ConsoleProviderConfig())
        
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
        
        val provider = StubProvider(ConsoleProviderConfig().apply {
            metricsSpec = MetricsCollectorSpec.Explicit(explicitCollector)
        })
        
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
        
        val provider = StubProvider(ConsoleProviderConfig().apply {
            metricsSpec = MetricsCollectorSpec.NoOp
        })
        
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

        val provider = RecordingProvider(ConsoleProviderConfig().apply {
            enableInternalMetricsLogging = true
        })

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

        val provider = StubProvider(ConsoleProviderConfig().apply {
            metricsSpec = MetricsCollectorSpec.Explicit(explicitCollector, inheritParent = true)
        })

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

        val provider = RecordingProvider(ConsoleProviderConfig().apply {
            metricsSpec = MetricsCollectorSpec.Inherit
            enableInternalMetricsLogging = true
        })

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
}
