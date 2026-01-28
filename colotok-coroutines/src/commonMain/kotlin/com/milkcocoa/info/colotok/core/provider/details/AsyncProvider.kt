package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * An interface for providers that support both synchronous and asynchronous logging operations.
 * 
 * This interface extends Provider and implements the synchronous `write` methods by calling
 * the corresponding asynchronous `writeAsync` methods wrapped in the `blocking` function.
 * 
 * Note that on Kotlin/JS platform, the synchronous methods will not actually block due to
 * platform limitations, and will return immediately while the logging happens asynchronously.
 */
abstract class AsyncProvider: Provider, AutoCloseable {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val channel = Channel<LogRecord>(
        capacity = Channel.BUFFERED
    )
    private val job = coroutineScope.launch {
        try {
            for (record in channel) {
                writeAsync(record)
            }
        } finally {
            onClosed()
        }
    }

    abstract suspend fun writeAsync(record: LogRecord)
    override fun write(record: LogRecord) {
        channel.trySend(record)
    }

    override fun close() {
        channel.close()
    }

    suspend fun join() {
        channel.close()
        job.join()
    }

    /**
     * チャンネルが閉じられ、すべてのログレコードが処理された後に呼び出されます。
     * バッファのフラッシュなどのクリーンアップ処理をここに記述します。
     */
    protected open suspend fun onClosed() {
        // デフォルトでは何もしない
    }
}
