package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.coroutines.blocking
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration

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
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND
): Provider(onBufferOverflow) {
    constructor(): this(BufferOverflow.SUSPEND)
    suspend fun writeAsync(record: LogRecord) {
        channel.send(record)
    }
}
