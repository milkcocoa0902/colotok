package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import kotlinx.coroutines.delay
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
    
    @Test
    fun testForceShutdownBlocksUntilFinished() {
        val slowProvider = SlowProvider()
        val logger = ColotokLogger("force-test") {
            providers = listOf(slowProvider)
        }
        
        logger.info("slow message")
        
        // forceShutdownを呼ぶ。これが完了するまでブロックするはず。
        val startTime = System.currentTimeMillis()
        logger.forceShutdown()
        val endTime = System.currentTimeMillis()
        
        // 処理が終わっていることを確認
        Assertions.assertTrue(slowProvider.isFinished.get())
        Assertions.assertTrue(slowProvider.messageReceived.get())
        
        // delay(500) していたが、forceShutdownによってキャンセルされるため、500ms待たずに終了するはず
        val duration = endTime - startTime
        println("Duration: $duration ms")
        Assertions.assertTrue(duration < 500, "forceShutdown should be immediate and not wait for slow provider to finish")
    }
}
