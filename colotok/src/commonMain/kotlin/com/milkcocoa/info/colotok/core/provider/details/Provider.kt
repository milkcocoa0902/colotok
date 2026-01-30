package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface IProvider: AutoCloseable {
    val config: ProviderConfig
    open val coroutineScope: CoroutineScope
    val channel: Channel<LogRecord>
    val job: Job

    fun write(record: LogRecord)
    suspend fun flush(timeout: Duration = 1000.milliseconds)
    suspend fun onFlush(){}
    suspend fun onMessage(record: LogRecord)
    fun onClosed(){}
}

abstract class Provider(
    override val config: ProviderConfig,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND
): IProvider {
    private val handler = CoroutineExceptionHandler { _, throwable ->
        println("Failed to process async log: ${throwable.message}")
    }

    override val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)
    override val channel = Channel<LogRecord>(
        capacity = Channel.BUFFERED,
        onBufferOverflow = onBufferOverflow
    )
    override val job = coroutineScope.launch {
        try {
            for (record in channel) {
                if (record is LogRecord.Pin) {
                    runCatching{ onFlush() }
                    record.deferred.complete(Unit)
                    continue
                }
                runCatching { onMessage(record) }
            }
        } finally {
            onFlush()
            onClosed()
        }
    }


    override fun write(record: LogRecord) {
        if (record.level.isEnabledFor(config.level)) {
            channel.trySend(record)
        }
    }


    override fun close() {
        channel.close()
    }
    /**
     * 現在キューにあるすべてのログが処理され、
     * かつ Provider 固有のバッファがフラッシュされるまで待機します。
     */
    override suspend fun flush(timeout: Duration) {
        val flushToken = CompletableDeferred<Unit>()
        channel.send(LogRecord.Pin(flushToken))
        withContext(Dispatchers.Default.limitedParallelism(1)){
            withTimeout(timeout){
                flushToken.await()
            }
        }
    }

    suspend fun join() {
        channel.close()
        job.join()
    }
}