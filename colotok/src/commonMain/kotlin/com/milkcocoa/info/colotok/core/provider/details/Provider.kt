package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

abstract class Provider(
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND
): AutoCloseable {
    constructor(): this(BufferOverflow.SUSPEND)
    private val handler = CoroutineExceptionHandler { _, throwable ->
        println("Failed to process async log: ${throwable.message}")
    }

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)
    protected val channel = Channel<LogRecord>(
        capacity = Channel.BUFFERED,
        onBufferOverflow = onBufferOverflow
    )
    private val job = coroutineScope.launch {
        try {
            for (record in channel) {
                if (record is LogRecord.Pin) {
                    onFlush()
                    record.deferred.complete(Unit)
                    continue
                }
                onMessage(record)
            }
        } finally {
            onFlush()
            onClosed()
        }
    }

    /**
     * チャンネルが閉じられ、すべてのログレコードが処理された後に呼び出されます。
     * バッファのフラッシュなどのクリーンアップ処理をここに記述します。
     */
    protected open fun onClosed() {
        // デフォルトでは何もしない
    }

    open fun write(record: LogRecord) {
        channel.trySend(record)
    }

    abstract fun onMessage(record: LogRecord)

    override fun close() {
        channel.close()
    }
    /**
     * 現在キューにあるすべてのログが処理され、
     * かつ Provider 固有のバッファがフラッシュされるまで待機します。
     */
    suspend fun flush() {
        val flushToken = CompletableDeferred<Unit>()
        channel.send(LogRecord.Pin(flushToken))
        flushToken.await()
    }

    /**
     * 派生クラスで実装される、固有のバッファフラッシュ処理です。
     * FileProvider であればディスクへの書き込み、LokiProvider であれば HTTP 送信をここで行います。
     */
    protected open suspend fun onFlush() {
        // デフォルトでは何もしない
    }

    suspend fun join() {
        channel.close()
        job.join()
    }
}