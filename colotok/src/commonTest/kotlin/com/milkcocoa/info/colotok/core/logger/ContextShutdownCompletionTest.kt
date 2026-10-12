package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.core.provider.details.ProviderClosedException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ContextShutdownCompletionTest {
    @Test
    fun context_requests_every_provider_then_reports_failure_only_after_all_resources_are_released() =
        runTest {
            val expected = IllegalStateException("first provider close failed")
            val entered = CompletableDeferred<Unit>()
            val cleaning = CompletableDeferred<Unit>()
            val releaseCleanup = CompletableDeferred<Unit>()
            val firstClosed = CompletableDeferred<Unit>()
            val notified = CompletableDeferred<Result<Unit>>()
            var secondCloseCount = 0
            val first =
                object : Provider(ConsoleProviderConfig()) {
                    override suspend fun onMessage(record: LogRecord) = Unit

                    override fun onClosed() {
                        firstClosed.complete(Unit)
                        throw expected
                    }
                }
            val second =
                object : Provider(ConsoleProviderConfig()) {
                    override suspend fun onMessage(record: LogRecord) {
                        entered.complete(Unit)
                        try {
                            awaitCancellation()
                        } finally {
                            withContext(NonCancellable) {
                                cleaning.complete(Unit)
                                releaseCleanup.await()
                            }
                        }
                    }

                    override fun onClosed() {
                        secondCloseCount++
                    }
                }
            // No active logger is necessary for context-owned providers to stop.
            val context = ColotokLoggerContext().addProvider(first).addProvider(second)

            try {
                second.write(LogRecord.PlainText("test", "message", LogLevel.INFO, emptyMap()))
                entered.await()
                context.forceShutdown(this) { assertTrue(notified.complete(it), "Context must notify once") }
                cleaning.await()
                firstClosed.await()

                assertTrue(second.job.isCancelled)
                assertFalse(notified.isCompleted)
                assertEquals(0, secondCloseCount)

                releaseCleanup.complete(Unit)
                assertSame(expected, notified.await().exceptionOrNull())
                assertEquals(1, secondCloseCount)
                assertSame(expected, assertFailsWith<IllegalStateException> { first.join() })
                assertFailsWith<ProviderClosedException> { second.join() }

                val repeated = CompletableDeferred<Result<Unit>>()
                context.forceShutdown(this) { assertTrue(repeated.complete(it)) }
                assertSame(expected, repeated.await().exceptionOrNull())
                assertEquals(1, secondCloseCount)
            } finally {
                releaseCleanup.complete(Unit)
                context.forceShutdown()
                runCatching { first.join() }
                runCatching { second.join() }
            }
        }

    @Test
    fun empty_context_still_notifies_success() =
        runTest {
            val notified = CompletableDeferred<Result<Unit>>()
            ColotokLoggerContext().forceShutdown(this) { assertTrue(notified.complete(it)) }
            notified.await().getOrThrow()
        }
}