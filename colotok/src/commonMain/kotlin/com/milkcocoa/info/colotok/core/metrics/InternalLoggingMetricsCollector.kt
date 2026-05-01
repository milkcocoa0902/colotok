package com.milkcocoa.info.colotok.core.metrics

import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.Provider

/**
 * A [MetricsCollector] that logs metrics using the provider itself.
 */
class InternalLoggingMetricsCollector(private val provider: Provider) : MetricsCollector {
    override fun incrementLogCount(level: Level, providerName: String) {
        provider.write(LogRecord.Metrics(
            name = "Metrics",
            msg = "[METRICS] LogCount increased: level=$level, provider=$providerName",
            level = LogLevel.DEBUG,
            attr = emptyMap()
        ))
    }

    override fun incrementErrorCount(providerName: String, errorType: String) {
        provider.write(LogRecord.Metrics(
            name = "Metrics",
            msg = "[METRICS] Error occurred: provider=$providerName, type=$errorType",
            level = LogLevel.ERROR,
            attr = emptyMap()
        ))
    }

    override fun updateBufferSize(providerName: String, size: Int) {
        provider.write(LogRecord.Metrics(
            name = "Metrics",
            msg = "[METRICS] BufferSize updated: provider=$providerName, size=$size",
            level = LogLevel.DEBUG,
            attr = emptyMap()
        ))
    }

    override fun recordWriteDuration(providerName: String, durationMs: Long) {
        provider.write(LogRecord.Metrics(
            name = "Metrics",
            msg = "[METRICS] Write duration: provider=$providerName, duration=${durationMs}ms",
            level = LogLevel.DEBUG,
            attr = emptyMap()
        ))
    }
}
