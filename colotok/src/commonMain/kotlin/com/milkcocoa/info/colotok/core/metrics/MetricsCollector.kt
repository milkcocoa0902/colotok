package com.milkcocoa.info.colotok.core.metrics

import com.milkcocoa.info.colotok.core.level.Level

/**
 * Interface for collecting metrics from Colotok.
 */
interface MetricsCollector {
    /**
     * Increment the count of logs processed.
     * @param level The level of the log record.
     * @param providerName The simple name of the provider.
     */
    fun incrementLogCount(level: Level, providerName: String)

    /**
     * Increment the count of errors occurred.
     * @param providerName The simple name of the provider.
     * @param errorType A string representing the type of error (e.g., "buffer_full", "network_error").
     */
    fun incrementErrorCount(providerName: String, errorType: String)

    /**
     * Update the current buffer size.
     * @param providerName The simple name of the provider.
     * @param size The current number of items in the buffer.
     */
    fun updateBufferSize(providerName: String, size: Int)

    /**
     * Record the duration of a write operation.
     * @param providerName The simple name of the provider.
     * @param durationMs The duration in milliseconds.
     */
    fun recordWriteDuration(providerName: String, durationMs: Long)
}

/**
 * A No-Op implementation of [MetricsCollector].
 */
object NoOpMetricsCollector : MetricsCollector {
    override fun incrementLogCount(level: Level, providerName: String) {}
    override fun incrementErrorCount(providerName: String, errorType: String) {}
    override fun updateBufferSize(providerName: String, size: Int) {}
    override fun recordWriteDuration(providerName: String, durationMs: Long) {}
}

/**
 * Specification for how a provider should obtain its [MetricsCollector].
 */
sealed class MetricsCollectorSpec {
    /**
     * Do not collect metrics.
     */
    object NoOp : MetricsCollectorSpec()

    /**
     * Inherit the collector from the [ColotokLoggerContext].
     */
    object Inherit : MetricsCollectorSpec()

    /**
     * Use an explicitly provided [MetricsCollector].
     * @param collector The collector to use.
     * @param inheritParent If true, also use the inherited collector from the context.
     */
    data class Explicit(val collector: MetricsCollector, val inheritParent: Boolean = false) : MetricsCollectorSpec()
}

/**
 * A [MetricsCollector] that delegates to multiple collectors.
 */
class CompositeMetricsCollector(private val collectors: List<MetricsCollector>) : MetricsCollector {

    override fun incrementLogCount(level: Level, providerName: String) {
        collectors.forEach { it.incrementLogCount(level, providerName) }
    }

    override fun incrementErrorCount(providerName: String, errorType: String) {
        collectors.forEach { it.incrementErrorCount(providerName, errorType) }
    }

    override fun updateBufferSize(providerName: String, size: Int) {
        collectors.forEach { it.updateBufferSize(providerName, size) }
    }

    override fun recordWriteDuration(providerName: String, durationMs: Long) {
        collectors.forEach { it.recordWriteDuration(providerName, durationMs) }
    }
}
