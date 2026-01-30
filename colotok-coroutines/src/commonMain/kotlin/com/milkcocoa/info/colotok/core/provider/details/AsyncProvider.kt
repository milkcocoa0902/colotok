package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.coroutines.channels.BufferOverflow

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * An interface for providers that support both synchronous and asynchronous logging operations.
 * 
 * This interface extends Provider and implements the synchronous `write` methods by calling
 * the corresponding asynchronous `writeAsync` methods wrapped in the `blocking` function.
 * 
 * Note that on Kotlin/JS platform, the synchronous methods will not actually block due to
 * platform limitations, and will return immediately while the logging happens asynchronously.
 */
abstract class AsyncProvider(
    config: AsyncProviderConfig,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND
): Provider(config, onBufferOverflow) {
    private val buffer = mutableListOf<LogRecord>()
    private val mutex = Mutex()

    final override suspend fun onMessage(record: LogRecord) {
        val batchToSend = mutex.withLock {
            buffer.add(record)
            if (buffer.size >= (config as AsyncProviderConfig).bufferSize) {
                val copy = buffer.toList()
                buffer.clear()
                copy
            } else {
                null
            }
        }

        batchToSend?.let { publishWithRetry(it) }
    }

    abstract suspend fun onPublish(records: List<LogRecord>)

    private suspend fun publishWithRetry(records: List<LogRecord>) {
        runCatching { onPublish(records) }
            .onFailure {
                println("Failed to publish logs: ${it.message}")
            }
    }

    override suspend fun onFlush() {
        val remaining = mutex.withLock {
            val copy = buffer.toList()
            buffer.clear()
            copy
        }
        if (remaining.isNotEmpty()) {
            publishWithRetry(remaining)
        }
    }

    suspend fun writeAsync(record: LogRecord) {
        if (record.level.isEnabledFor(config.level)) {
            channel.send(record)
        }
    }
}
