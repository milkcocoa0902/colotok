package com.milkcocoa.info.colotok.core.metrics

import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.logger.ColotokLoggerContext
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.core.logger.LogRecord
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
}
