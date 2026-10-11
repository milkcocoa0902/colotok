package quality.benchmarks.platforms

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.ColotokLoggerContext
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.logger.eventCallerSnapshot
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class CallerSnapshotNativeTest {
    private class RecordingProvider : Provider(ConsoleProviderConfig()) {
        lateinit var record: LogRecord

        override suspend fun onMessage(record: LogRecord) {
            this.record = record
        }
    }

    @Test
    fun direct_record_retains_the_constructor_call_site() {
        val record = LogRecord.PlainText("caller", "message", LogLevel.INFO, emptyMap())

        assertTrue(
            record.eventCallerSnapshot.contains("direct_record_retains_the_constructor_call_site"),
            record.eventCallerSnapshot
        )
    }

    @Test
    fun logger_retains_the_call_site_after_worker_processing() =
        runBlocking {
            val provider = RecordingProvider()
            val logger = ColotokLoggerContext().addProvider(provider).getLogger("caller")

            try {
                logger.info("message")
                provider.flush()

                assertTrue(
                    provider.record.eventCallerSnapshot.contains(
                        "logger_retains_the_call_site_after_worker_processing"
                    ),
                    provider.record.eventCallerSnapshot
                )
            } finally {
                provider.close()
                provider.join()
            }
        }

    @Test
    fun level_scoped_logger_retains_the_user_call_site() =
        runBlocking {
            val provider = RecordingProvider()
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