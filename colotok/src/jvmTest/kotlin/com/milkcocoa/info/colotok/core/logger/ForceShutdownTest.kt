package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

class ForceShutdownTest {
    private class SlowProvider : Provider(
        config = ConsoleProviderConfig()
    ) {
        val isFinished = AtomicBoolean(false)
        val messageReceived = AtomicBoolean(false)

        override suspend fun onMessage(record: LogRecord) {
            messageReceived.set(true)
            delay(500) // 処理を遅延させる
        }

        override fun onClosed() {
            isFinished.set(true)
        }
    }

    private class HangingFlushProvider : Provider(
        config = ConsoleProviderConfig()
    ) {
        val isClosed = AtomicBoolean(false)

        override suspend fun onMessage(record: LogRecord) = Unit

        override suspend fun onFlush() {
            delay(Long.MAX_VALUE)
        }

        override fun onClosed() {
            isClosed.set(true)
        }
    }

    @Test
    fun testForceShutdownBlocksUntilFinished() {
        val slowProvider = SlowProvider()
        val logger =
            ColotokLogger("force-test") {
                providers = listOf(slowProvider)
            }

        logger.info("slow message")

        // forceShutdownを呼ぶ。これが完了するまでブロックするはず。
        val startTime = System.currentTimeMillis()
        logger.forceShutdown()
        val endTime = System.currentTimeMillis()

        // close hook should run, but forceShutdown is allowed to drop queued records.
        Assertions.assertTrue(slowProvider.isFinished.get())

        // delay(500) していたが、forceShutdownによってキャンセルされるため、500ms待たずに終了するはず
        val duration = endTime - startTime
        Assertions.assertTrue(
            duration < 500,
            "forceShutdown should be immediate and not wait for slow provider to finish"
        )
    }

    @Test
    fun force_does_not_start_or_wait_for_hanging_flush() {
        val provider = HangingFlushProvider()

        val startTime = System.currentTimeMillis()
        provider.forceShutdown()
        val duration = System.currentTimeMillis() - startTime

        Assertions.assertTrue(provider.isClosed.get())
        Assertions.assertTrue(duration < 500, "forceShutdown must not run the graceful flush hook")
        Assertions.assertThrows(
            com.milkcocoa.info.colotok.core.provider.details.ProviderClosedException::class.java
        ) {
            runBlocking { provider.flush() }
        }
    }

    @Test
    fun force_cancels_hanging_graceful_final_flush() =
        runBlocking {
            val flushStarted = CompletableDeferred<Unit>()
            val provider =
                object : Provider(ConsoleProviderConfig()) {
                    override suspend fun onMessage(record: LogRecord) = Unit

                    override suspend fun onFlush() {
                        flushStarted.complete(Unit)
                        delay(Long.MAX_VALUE)
                    }
                }
            provider.close()
            flushStarted.await()

            val startTime = System.currentTimeMillis()
            provider.forceShutdown()
            val duration = System.currentTimeMillis() - startTime

            Assertions.assertTrue(duration < 500, "forceShutdown must cancel a final flush already in progress")
        }
}