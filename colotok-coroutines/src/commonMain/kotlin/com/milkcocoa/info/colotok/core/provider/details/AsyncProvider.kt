package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.ClosedSendChannelException

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A provider that retains accepted records in a bounded buffer and publishes them in batches.
 *
 * [Provider.write] keeps its non-blocking enqueue semantics. [writeAsync] is the suspending
 * alternative for callers that need to wait until the provider channel accepts a record.
 * Publishing is attempted at threshold multiples and at the retention limit; failed batches
 * remain buffered for a later trigger or the final flush performed by graceful shutdown.
 */
abstract class AsyncProvider(
    config: AsyncProviderConfig,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND
): Provider(validateAsyncProviderConfig(config), onBufferOverflow) {
    private val buffer = mutableListOf<LogRecord>()
    private val mutex = Mutex()
    private val publishThreshold = config.bufferSize
    private val effectiveCapacity = calculateEffectiveCapacity(config.bufferSize)
    private val providerName = this::class.simpleName ?: "unknown"

    final override suspend fun onMessage(record: LogRecord) {
        val result = mutex.withLock {
            if (buffer.size == effectiveCapacity) {
                BufferResult.Dropped
            } else {
                buffer.add(record)
                if (record !is LogRecord.Metrics) updateBufferSizeBestEffort(buffer.size)

                val shouldPublish =
                    buffer.size % publishThreshold == 0 || buffer.size == effectiveCapacity
                if (shouldPublish) BufferResult.Publish(buffer.toList()) else BufferResult.Buffered
            }
        }

        when (result) {
            BufferResult.Buffered -> Unit
            BufferResult.Dropped -> if (record !is LogRecord.Metrics) {
                collectMetricsBestEffort {
                    effectiveMetricsCollector.incrementErrorCount(providerName, "retention_limit_reached")
                }
            }
            is BufferResult.Publish -> publishAutomatically(
                records = result.records,
                reportMetrics = result.records.any { it !is LogRecord.Metrics },
            )
        }
    }

    abstract suspend fun onPublish(records: List<LogRecord>)

    private suspend fun publishAutomatically(records: List<LogRecord>, reportMetrics: Boolean) {
        try {
            onPublish(records)
            removePublishedPrefix(records, reportMetrics)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            if (reportMetrics) {
                collectMetricsBestEffort {
                    effectiveMetricsCollector.incrementErrorCount(providerName, "publish_failed")
                }
            }
        }
    }

    private suspend fun removePublishedPrefix(records: List<LogRecord>, reportMetrics: Boolean) {
        mutex.withLock {
            buffer.subList(0, records.size).clear()
            if (reportMetrics) updateBufferSizeBestEffort(buffer.size)
        }
    }

    override suspend fun onFlush() {
        val remaining = mutex.withLock { buffer.toList() }
        if (remaining.isNotEmpty()) {
            try {
                onPublish(remaining)
                removePublishedPrefix(
                    records = remaining,
                    reportMetrics = remaining.any { it !is LogRecord.Metrics },
                )
            } catch (throwable: Throwable) {
                if (remaining.any { it !is LogRecord.Metrics }) {
                    collectMetricsBestEffort {
                        effectiveMetricsCollector.incrementErrorCount(providerName, "publish_failed")
                    }
                }
                throw throwable
            }
        }
    }

    private fun updateBufferSizeBestEffort(size: Int) {
        collectMetricsBestEffort {
            effectiveMetricsCollector.updateBufferSize(providerName, size)
        }
    }

    private inline fun collectMetricsBestEffort(block: () -> Unit) {
        try {
            block()
        } catch (_: Throwable) {
            // Metrics are diagnostic and must not stop the provider worker.
        }
    }

    suspend fun writeAsync(record: LogRecord) {
        if (!record.level.isEnabledFor(config.level)) return

        try {
            channel.send(record)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            if (record !is LogRecord.Metrics) {
                collectMetricsBestEffort {
                    val errorType =
                        if (throwable is ClosedSendChannelException) "provider_closed" else "provider_failed"
                    effectiveMetricsCollector.incrementErrorCount(providerName, errorType)
                }
            }
            if (throwable !is ClosedSendChannelException) throw throwable
            return
        }

        if (record !is LogRecord.Metrics) {
            collectMetricsBestEffort {
                effectiveMetricsCollector.incrementLogCount(record.level, providerName)
            }
        }
    }

    private sealed interface BufferResult {
        data object Buffered : BufferResult
        data object Dropped : BufferResult
        data class Publish(val records: List<LogRecord>) : BufferResult
    }
}

private const val MAX_BUFFER_SIZE = 4_096

private fun validateAsyncProviderConfig(config: AsyncProviderConfig): AsyncProviderConfig = config.apply {
    require(bufferSize in 1..MAX_BUFFER_SIZE) {
        "bufferSize must be between 1 and $MAX_BUFFER_SIZE, but was $bufferSize"
    }
}

private fun calculateEffectiveCapacity(bufferSize: Int): Int =
    if (bufferSize > MAX_BUFFER_SIZE / 4) MAX_BUFFER_SIZE else bufferSize * 4
