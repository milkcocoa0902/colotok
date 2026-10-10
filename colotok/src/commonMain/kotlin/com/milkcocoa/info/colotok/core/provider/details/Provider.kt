package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.metrics.MetricsCollector
import com.milkcocoa.info.colotok.core.metrics.NoOpMetricsCollector
import com.milkcocoa.info.colotok.core.metrics.collectBestEffort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class ProviderClosedException(
    message: String = "Provider is closed"
) : IllegalStateException(message)

interface IProvider : AutoCloseable {
    val config: ProviderConfig
    val coroutineScope: CoroutineScope
    val channel: Channel<LogRecord>
    val job: Job

    /**
     * Attempts to enqueue [record] without blocking the caller.
     *
     * Records below the configured level are ignored. The default `SUSPEND`
     * overflow policy does not suspend this synchronous API: a full bounded queue
     * rejects the newest record. Configured drop policies retain channel semantics,
     * and every policy rejects records after this provider stops being open.
     */
    fun write(record: LogRecord)

    suspend fun flush(timeout: Duration = 1000.milliseconds)

    suspend fun onFlush() {}

    suspend fun onMessage(record: LogRecord)

    fun onClosed() {}

    /** Requests cancellation without waiting for worker termination or resource release. */
    fun forceShutdown()

    /** Notifies in this provider's scope after worker termination and resource release. */
    fun forceShutdown(onComplete: suspend (Result<Unit>) -> Unit) {
        forceShutdown(coroutineScope, onComplete)
    }

    /**
     * Requests cancellation even if [callbackScope] is cancelled.
     *
     * Expected cancellation is a successful result; processing or cleanup failures
     * are reported to [onComplete]. Notification can be cancelled with [callbackScope],
     * and callback exceptions follow that scope's normal exception handling.
     */
    fun forceShutdown(
        callbackScope: CoroutineScope,
        onComplete: suspend (Result<Unit>) -> Unit
    )
}

abstract class Provider(
    override val config: ProviderConfig,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND
) : IProvider {
    public var effectiveMetricsCollector: MetricsCollector = NoOpMetricsCollector

    private enum class State {
        OPEN,
        CLOSING,
        CLOSED,
        CANCELLED,
        FAILED
    }

    private val state = MutableStateFlow(State.OPEN)
    private val failure = MutableStateFlow<Throwable?>(null)
    private val closedHookInvoked = MutableStateFlow(false)
    private val termination = CompletableDeferred<Unit>()

    private class WorkerContext(val provider: Provider) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<WorkerContext>
    }

    private val workerContext = WorkerContext(this)

    override val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override val channel =
        Channel<LogRecord>(
            capacity = Channel.BUFFERED,
            onBufferOverflow = onBufferOverflow
        )
    override val job =
        coroutineScope.launch(workerContext) {
            try {
                for (record in channel) {
                    if (record is LogRecord.Pin) {
                        try {
                            onFlush()
                            record.deferred.complete(Unit)
                        } catch (throwable: Throwable) {
                            record.deferred.completeExceptionally(throwable)
                            throw throwable
                        }
                        continue
                    }
                    onMessage(record)
                }
            } catch (throwable: Throwable) {
                if (throwable !is CancellationException || state.value != State.CANCELLED) {
                    fail(throwable)
                }
            } finally {
                if (state.value == State.CLOSING) {
                    try {
                        onFlush()
                    } catch (throwable: Throwable) {
                        if (throwable !is CancellationException || state.value != State.CANCELLED) {
                            fail(throwable)
                        }
                    }
                }

                withContext(NonCancellable) {
                    releaseResources()
                }
            }
        }.also { worker ->
            worker.invokeOnCompletion { cause ->
                if (cause != null && (cause !is CancellationException || state.value != State.CANCELLED)) {
                    fail(cause)
                }
                if (closedHookInvoked.value) {
                    termination.complete(Unit)
                } else {
                    releaseResourcesAfterUnstartedWorker()
                }
            }
        }

    // Cancellation before launch starts skips the worker's finally. Keep arbitrary
    // close hooks out of completion handlers, and guarantee this finite cleanup
    // starts even if the provider's parent scope was also cancelled.
    @OptIn(DelicateCoroutinesApi::class)
    private fun releaseResourcesAfterUnstartedWorker() {
        coroutineScope.launch(workerContext, start = CoroutineStart.ATOMIC) {
            try {
                releaseResources()
            } catch (throwable: Throwable) {
                fail(throwable)
            } finally {
                termination.complete(Unit)
            }
        }
    }

    private fun releaseResources() {
        closeResources()

        val terminalFailure = failure.value
        while (true) {
            val pending = channel.tryReceive().getOrNull() ?: break
            if (pending is LogRecord.Pin) {
                pending.deferred.completeExceptionally(terminalFailure ?: ProviderClosedException())
            }
        }

        when (state.value) {
            State.CLOSING, State.OPEN -> state.value = State.CLOSED
            else -> Unit
        }
    }

    internal fun onTermination(onComplete: (Result<Unit>) -> Unit) {
        termination.invokeOnCompletion {
            val result = failure.value?.let { Result.failure<Unit>(it) } ?: Result.success(Unit)
            onComplete(result)
        }
    }

    private suspend fun ensureOutsideWorker() {
        check(termination.isCompleted || currentCoroutineContext()[WorkerContext]?.provider !== this) {
            "A provider worker cannot wait for its own flush or termination"
        }
    }

    private fun fail(throwable: Throwable) {
        if (failure.compareAndSet(null, throwable)) {
            state.value = State.FAILED
            channel.close(throwable)
        }
    }

    private fun closeResources() {
        if (!closedHookInvoked.compareAndSet(expect = false, update = true)) return
        try {
            onClosed()
        } catch (throwable: Throwable) {
            fail(throwable)
        }
    }

    private fun closedException(): ProviderClosedException =
        ProviderClosedException("Provider is ${state.value.name.lowercase()}")

    override fun write(record: LogRecord) {
        if (!record.level.isEnabledFor(config.level)) return

        val observedState = state.value
        val sendResult = if (observedState == State.OPEN) channel.trySend(record) else null
        if (sendResult?.isSuccess == true) {
            if (record !is LogRecord.Metrics) {
                effectiveMetricsCollector.collectBestEffort {
                    incrementLogCount(record.level, this@Provider::class.simpleName ?: "unknown")
                }
            }
            return
        }

        if (record !is LogRecord.Metrics) {
            val errorType =
                when {
                    failure.value != null || sendResult?.exceptionOrNull() != null -> "provider_failed"
                    observedState != State.OPEN || sendResult?.isClosed == true -> "provider_closed"
                    else -> "buffer_full"
                }
            effectiveMetricsCollector.collectBestEffort {
                incrementErrorCount(this@Provider::class.simpleName ?: "unknown", errorType)
            }
        }
    }

    override fun close() {
        if (state.compareAndSet(State.OPEN, State.CLOSING)) {
            channel.close()
        }
    }

    override fun forceShutdown() {
        while (true) {
            when (val current = state.value) {
                State.CLOSED -> return
                State.CANCELLED, State.FAILED -> break
                else -> if (state.compareAndSet(current, State.CANCELLED)) break
            }
        }
        channel.close()
        job.cancel()
    }

    override fun forceShutdown(
        callbackScope: CoroutineScope,
        onComplete: suspend (Result<Unit>) -> Unit
    ) {
        forceShutdown()
        onTermination { result ->
            notifyShutdown(callbackScope, result, onComplete)
        }
    }

    /**
     * 現在キューにあるすべてのログが処理され、
     * かつ Provider 固有のバッファがフラッシュされるまで待機します。
     */
    override suspend fun flush(timeout: Duration) {
        ensureOutsideWorker()
        failure.value?.let { throw it }
        if (state.value != State.OPEN) throw closedException()

        val flushToken = CompletableDeferred<Unit>()
        val flushPin = LogRecord.Pin(flushToken)

        try {
            withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(timeout) {
                    val sendResult = channel.trySend(flushPin)
                    when {
                        sendResult.isSuccess -> Unit
                        sendResult.isClosed -> throw failure.value ?: closedException()
                        else -> channel.send(flushPin)
                    }
                    flushToken.await()
                }
            }
        } catch (_: ClosedSendChannelException) {
            throw failure.value ?: closedException()
        } catch (throwable: Throwable) {
            // Await may recover/copy an exception on JVM. Re-throw the provider's
            // first recorded failure so every lifecycle entry point exposes the
            // same original cause.
            throw failure.value ?: throwable
        }
    }

    suspend fun join() {
        ensureOutsideWorker()
        if (state.value == State.OPEN) close()
        termination.await()
        failure.value?.let { throw it }
        if (state.value == State.CANCELLED) throw closedException()
    }
}