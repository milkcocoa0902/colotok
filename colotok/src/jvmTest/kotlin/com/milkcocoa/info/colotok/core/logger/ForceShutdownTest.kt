package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

class ForceShutdownTest {
    @Test
    fun completed_provider_does_not_run_a_blocking_unconfined_callback_on_the_requesting_thread() =
        runBlocking {
            val provider =
                object : Provider(ConsoleProviderConfig()) {
                    override suspend fun onMessage(record: LogRecord) = Unit
                }
            provider.close()
            provider.join()

            val callbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val callbackEntered = CountDownLatch(1)
            val releaseCallback = CountDownLatch(1)
            val callbackFinished = CountDownLatch(1)
            val result = CompletableFuture<Result<Unit>>()
            val caller = Executors.newSingleThreadExecutor()

            try {
                val request =
                    caller.submit {
                        provider.forceShutdown(callbackScope) {
                            result.complete(it)
                            callbackEntered.countDown()
                            // A user callback may have a synchronous prefix. It must
                            // not block the request or a provider completion handler.
                            try {
                                releaseCallback.await()
                            } finally {
                                callbackFinished.countDown()
                            }
                        }
                    }
                request.get(5, TimeUnit.SECONDS)
                assertTrue(callbackEntered.await(5, TimeUnit.SECONDS))
                assertTrue(result.get(5, TimeUnit.SECONDS).isSuccess)
            } finally {
                releaseCallback.countDown()
                callbackFinished.await(5, TimeUnit.SECONDS)
                callbackScope.cancel()
                caller.shutdownNow()
            }
        }
}