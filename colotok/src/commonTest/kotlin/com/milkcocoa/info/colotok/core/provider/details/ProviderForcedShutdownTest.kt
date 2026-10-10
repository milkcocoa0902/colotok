package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ProviderForcedShutdownTest {
    private fun record() = LogRecord.PlainText("shutdown", "message", LogLevel.INFO, emptyMap())

    private suspend fun Provider.dispose() {
        forceShutdown()
        runCatching { join() }
    }

    @Test
    fun force_returns_before_cleanup_but_join_and_notification_wait_for_resource_release() =
        runTest {
            val entered = CompletableDeferred<Unit>()
            val cleaning = CompletableDeferred<Unit>()
            val releaseCleanup = CompletableDeferred<Unit>()
            val closed = CompletableDeferred<Unit>()
            val notified = CompletableDeferred<Result<Unit>>()
            val releaseNotification = CompletableDeferred<Unit>()
            val notificationFinished = CompletableDeferred<Unit>()
            val provider =
                object : Provider(ConsoleProviderConfig()) {
                    override suspend fun onMessage(record: LogRecord) {
                        entered.complete(Unit)
                        try {
                            awaitCancellation()
                        } finally {
                            withContext(NonCancellable) {
                                cleaning.complete(Unit)
                                releaseCleanup.await()
                                assertFalse(closed.isCompleted, "Resource must remain open during message cleanup")
                            }
                        }
                    }

                    override suspend fun onFlush() = error("Forced shutdown must not start a final flush")

                    override fun onClosed() {
                        closed.complete(Unit)
                    }
                }

            try {
                provider.write(record())
                entered.await()
                // Exercise the overload using the provider's own scope; the
                // notification must be a sibling, not a child of the worker.
                provider.forceShutdown {
                    notified.complete(it)
                    releaseNotification.await()
                    notificationFinished.complete(Unit)
                }
                cleaning.await()
                val joined =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        assertFailsWith<ProviderClosedException> { provider.join() }
                    }

                assertFalse(closed.isCompleted)
                assertFalse(notified.isCompleted)
                assertFalse(joined.isCompleted)

                releaseCleanup.complete(Unit)
                notified.await().getOrThrow()
                assertTrue(closed.isCompleted)
                assertTrue(provider.job.isCompleted)
                joined.await()
                assertFalse(notificationFinished.isCompleted, "join must not wait for the user callback")

                releaseNotification.complete(Unit)
                notificationFinished.await()
            } finally {
                releaseCleanup.complete(Unit)
                releaseNotification.complete(Unit)
                provider.dispose()
            }
        }

    @Test
    fun worker_can_request_its_own_shutdown_but_cannot_wait_for_its_own_lifecycle() =
        runTest {
            val selfWaitFailures = CompletableDeferred<List<Throwable?>>()
            val returned = CompletableDeferred<Unit>()
            val provider =
                object : Provider(ConsoleProviderConfig()) {
                    override suspend fun onMessage(record: LogRecord) {
                        // NonCancellable changes the current Job, but must not
                        // bypass the prohibition on waiting for this worker.
                        withContext(NonCancellable) {
                            selfWaitFailures.complete(
                                listOf(
                                    runCatching { flush() }.exceptionOrNull(),
                                    runCatching { join() }.exceptionOrNull()
                                )
                            )
                        }
                        forceShutdown()
                        returned.complete(Unit)
                    }
                }

            try {
                provider.write(record())
                selfWaitFailures.await().forEach {
                    assertTrue(it is IllegalStateException && it !is ProviderClosedException)
                }
                returned.await()
                assertFailsWith<ProviderClosedException> { provider.join() }
            } finally {
                provider.dispose()
            }
        }

    @Test
    fun force_cancels_a_graceful_final_flush_without_closing_resources_during_its_cleanup() =
        runTest {
            val flushing = CompletableDeferred<Unit>()
            val cleaning = CompletableDeferred<Unit>()
            val releaseCleanup = CompletableDeferred<Unit>()
            var flushCount = 0
            var closeCount = 0
            val provider =
                object : Provider(ConsoleProviderConfig()) {
                    override suspend fun onMessage(record: LogRecord) = Unit

                    override suspend fun onFlush() {
                        flushCount++
                        flushing.complete(Unit)
                        try {
                            awaitCancellation()
                        } finally {
                            withContext(NonCancellable) {
                                cleaning.complete(Unit)
                                releaseCleanup.await()
                                assertEquals(0, closeCount)
                            }
                        }
                    }

                    override fun onClosed() {
                        closeCount++
                    }
                }

            try {
                provider.close()
                flushing.await()
                provider.forceShutdown()
                cleaning.await()
                assertEquals(0, closeCount)

                releaseCleanup.complete(Unit)
                assertFailsWith<ProviderClosedException> { provider.join() }
                assertEquals(1, flushCount)
                assertEquals(1, closeCount)
            } finally {
                releaseCleanup.complete(Unit)
                provider.dispose()
            }
        }

    @Test
    fun close_failure_is_preserved_for_notifications_and_join_including_late_repeated_requests() =
        runTest {
            val expected = IllegalArgumentException("resource close failed")
            var closeCount = 0
            val provider =
                object : Provider(ConsoleProviderConfig()) {
                    override suspend fun onMessage(record: LogRecord) = Unit

                    override fun onClosed() {
                        closeCount++
                        throw expected
                    }
                }

            try {
                val first = CompletableDeferred<Result<Unit>>()
                provider.forceShutdown(this) { assertTrue(first.complete(it), "Notification must occur once") }
                assertSame(expected, first.await().exceptionOrNull())
                assertSame(expected, assertFailsWith<IllegalArgumentException> { provider.join() })

                val repeated = CompletableDeferred<Result<Unit>>()
                provider.forceShutdown(this) { assertTrue(repeated.complete(it), "Late notification must occur once") }
                assertSame(expected, repeated.await().exceptionOrNull())
                assertEquals(1, closeCount)
            } finally {
                provider.dispose()
            }
        }

    @Test
    fun cancelled_notification_scope_does_not_suppress_shutdown_or_resource_release() =
        runTest {
            val notified = CompletableDeferred<Unit>()
            val callbackScope = CoroutineScope(Job().apply { cancel() } + Dispatchers.Default)
            var closeCount = 0
            val provider =
                object : Provider(ConsoleProviderConfig()) {
                    override suspend fun onMessage(record: LogRecord) = Unit

                    override fun onClosed() {
                        closeCount++
                    }
                }

            try {
                provider.forceShutdown(callbackScope) { notified.complete(Unit) }
                assertFailsWith<ProviderClosedException> { provider.join() }
                assertEquals(1, closeCount)
                assertFalse(notified.isCompleted)
            } finally {
                provider.dispose()
            }
        }

    @Test
    fun notification_failure_belongs_to_callback_scope_and_does_not_replace_provider_result() =
        runTest {
            val expected = IllegalStateException("callback failed")
            val callbackFailure = CompletableDeferred<Throwable>()
            val handler = CoroutineExceptionHandler { _, throwable -> callbackFailure.complete(throwable) }
            val callbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined + handler)
            val provider =
                object : Provider(ConsoleProviderConfig()) {
                    override suspend fun onMessage(record: LogRecord) = Unit
                }

            try {
                provider.forceShutdown(callbackScope) {
                    it.getOrThrow()
                    assertTrue(provider.job.isCompleted)
                    throw expected
                }
                assertSame(expected, callbackFailure.await())
                assertFailsWith<ProviderClosedException> { provider.join() }
            } finally {
                callbackScope.cancel()
                provider.dispose()
            }
        }
}