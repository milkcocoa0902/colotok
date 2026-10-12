package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProviderShutdownStartupJsTest {
    @Test
    fun cancellation_before_worker_start_releases_resources_and_resolves_a_pending_flush_marker() =
        runTest {
            var flushCount = 0
            var closeCount = 0
            val provider =
                object : Provider(ConsoleProviderConfig()) {
                    override suspend fun onMessage(record: LogRecord) = Unit

                    override suspend fun onFlush() {
                        flushCount++
                    }

                    override fun onClosed() {
                        closeCount++
                    }
                }
            val marker = CompletableDeferred<Unit>()
            val notified = CompletableDeferred<Result<Unit>>()

            try {
                // Node is single-threaded: do not yield between construction and
                // cancellation, so the scheduled worker cannot start its body.
                assertTrue(provider.channel.trySend(LogRecord.Pin(marker)).isSuccess)
                provider.forceShutdown(this) { assertTrue(notified.complete(it)) }
                notified.await().getOrThrow()

                assertFailsWith<ProviderClosedException> { provider.join() }
                assertFailsWith<ProviderClosedException> { marker.await() }
                assertEquals(0, flushCount)
                assertEquals(1, closeCount)

                val repeated = CompletableDeferred<Result<Unit>>()
                provider.forceShutdown(this) { assertTrue(repeated.complete(it)) }
                repeated.await().getOrThrow()
                assertEquals(1, closeCount)
            } finally {
                provider.forceShutdown()
                runCatching { provider.join() }
            }
        }
}