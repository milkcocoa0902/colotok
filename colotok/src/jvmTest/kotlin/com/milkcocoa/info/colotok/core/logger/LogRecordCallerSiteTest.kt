package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class LogRecordCallerSiteTest {
    @Test
    fun direct_record_retains_the_constructor_call_site() {
        val record = LogRecord.PlainText("caller", "message", LogLevel.INFO, emptyMap())

        assertTrue(
            record.eventCallerSnapshot.startsWith(
                "c.m.i.c.c.l.LogRecordCallerSiteTest#direct_record_retains_the_constructor_call_site:"
            ),
            record.eventCallerSnapshot
        )
    }

    @Test
    fun a_class_with_a_logging_owner_prefix_is_still_a_caller() {
        val record = ColotokLoggerCallerFixture().createRecord()

        assertTrue(
            record.eventCallerSnapshot.startsWith("c.m.i.c.c.l.ColotokLoggerCallerFixture#createRecord:"),
            record.eventCallerSnapshot
        )
    }

    @Test
    fun level_scoped_logger_retains_the_user_call_site() =
        runBlocking {
            val provider =
                object : Provider(ConsoleProviderConfig()) {
                    lateinit var record: LogRecord

                    override suspend fun onMessage(record: LogRecord) {
                        this.record = record
                    }
                }
            val logger = ColotokLoggerContext().addProvider(provider).getLogger("caller")

            try {
                logger.atInfo { print("message") }
                provider.flush()

                assertTrue(
                    provider.record.eventCallerSnapshot.contains("level_scoped_logger_retains_the_user_call_site"),
                    provider.record.eventCallerSnapshot
                )
            } finally {
                provider.close()
                provider.join()
            }
        }
}

private class ColotokLoggerCallerFixture {
    fun createRecord(): LogRecord = LogRecord.PlainText("caller", "message", LogLevel.INFO, emptyMap())
}