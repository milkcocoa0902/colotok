package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.formatter.builtin.text.SimpleTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AsyncProviderTest {

    class TestAsyncProviderConfig : AsyncProviderConfig {
        override var level: Level = LogLevel.DEBUG
        override var formatter: Formatter = SimpleTextFormatter
        override var bufferSize: Int = 1
    }

    class TestAsyncProvider(config: TestAsyncProviderConfig) : AsyncProvider(config) {
        val processedRecords = mutableListOf<LogRecord>()

        override suspend fun onPublish(records: List<LogRecord>) {
            processedRecords.addAll(records)
        }
    }

    @Test
    fun testAsyncLogging() = runTest {
        val provider = TestAsyncProvider(TestAsyncProviderConfig())
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
