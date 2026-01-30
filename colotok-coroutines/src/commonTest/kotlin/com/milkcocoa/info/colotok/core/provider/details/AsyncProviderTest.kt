package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AsyncProviderTest {

    class TestAsyncProvider : AsyncProvider() {
        val processedRecords = mutableListOf<LogRecord>()

        override suspend fun writeAsync(record: LogRecord) {
            processedRecords.add(record)
        }
    }

    @Test
    fun testAsyncLogging() = runTest {
        val provider = TestAsyncProvider()
        val record1 = LogRecord.PlainText("test", "message 1", LogLevel.INFO, emptyMap())
        val record2 = LogRecord.PlainText("test", "message 2", LogLevel.INFO, emptyMap())

        provider.write(record1)
        provider.write(record2)

        // Wait for processing to complete
        provider.flush()

        assertEquals(2, provider.processedRecords.size)
        assertEquals(record1, provider.processedRecords[0])
        assertEquals(record2, provider.processedRecords[1])

        provider.join()
    }
}
