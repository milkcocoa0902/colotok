package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.metrics.MetricsCollector
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProviderWriteRejectionTest {
    private companion object {
        const val SATURATION_ATTEMPT_LIMIT = 10_000
    }

    private class RecordingMetricsCollector : MetricsCollector {
        var logCount = 0
        val errorTypes = mutableListOf<String>()

        override fun incrementLogCount(
            level: Level,
            providerName: String
        ) {
            logCount += 1
        }

        override fun incrementErrorCount(
            providerName: String,
            errorType: String
        ) {
            errorTypes += errorType
        }

        override fun updateBufferSize(
            providerName: String,
            size: Int
        ) = Unit

        override fun recordWriteDuration(
            providerName: String,
            durationMs: Long
        ) = Unit
    }

    private object ThrowingMetricsCollector : MetricsCollector {
        override fun incrementLogCount(
            level: Level,
            providerName: String
        ) = error("metrics failed")

        override fun incrementErrorCount(
            providerName: String,
            errorType: String
        ) = error("metrics failed")

        override fun updateBufferSize(
            providerName: String,
            size: Int
        ) = error("metrics failed")

        override fun recordWriteDuration(
            providerName: String,
            durationMs: Long
        ) = error("metrics failed")
    }

    @Test
    fun default_suspend_queue_accepts_fifo_prefix_and_rejects_newest_record() =
        runBlocking {
            val firstEntered = CompletableDeferred<Unit>()
            val releaseFirst = CompletableDeferred<Unit>()
            val provider =
                object : Provider(ConsoleProviderConfig()) {
                    val processed = mutableListOf<String>()

                    override suspend fun onMessage(record: LogRecord) {
                        val message = (record as LogRecord.PlainText).msg
                        if (message == "message-0") {
                            firstEntered.complete(Unit)
                            releaseFirst.await()
                        }
                        processed += message
                    }
                }
            val collector = RecordingMetricsCollector()
            provider.effectiveMetricsCollector = collector

            try {
                val accepted = mutableListOf("message-0")
                provider.write(plainText("message-0"))
                firstEntered.await()

                var nextIndex = 1
                var rejected = ""
                while (collector.errorTypes.isEmpty() && nextIndex <= SATURATION_ATTEMPT_LIMIT) {
                    val candidate = "message-${nextIndex++}"
                    provider.write(plainText(candidate))
                    if (collector.errorTypes.isEmpty()) {
                        accepted += candidate
                    } else {
                        rejected = candidate
                    }
                }

                assertTrue(rejected.isNotEmpty(), "queue did not reject within the finite attempt limit")
                assertEquals(listOf("buffer_full"), collector.errorTypes)
                assertEquals(accepted.size, collector.logCount)

                releaseFirst.complete(Unit)
                provider.join()

                assertEquals(accepted, provider.processed)
                assertFalse(rejected in provider.processed)
            } finally {
                releaseFirst.complete(Unit)
                runCatching { provider.forceShutdown() }
            }
        }

    @Test
    fun terminal_rejections_are_classified_by_close_cancel_and_failure() =
        runBlocking {
            val closedCollector = RecordingMetricsCollector()
            val closedProvider = recordingProvider(closedCollector)
            val cancelledCollector = RecordingMetricsCollector()
            val cancelledProvider = recordingProvider(cancelledCollector)
            val expectedFailure = IllegalStateException("message failed")
            val failedCollector = RecordingMetricsCollector()
            val failedProvider =
                object : Provider(ConsoleProviderConfig()) {
                    override suspend fun onMessage(record: LogRecord) {
                        throw expectedFailure
                    }
                }.also { it.effectiveMetricsCollector = failedCollector }

            try {
                closedProvider.close()
                closedProvider.write(plainText("closed"))
                closedProvider.join()

                cancelledProvider.forceShutdown()
                cancelledProvider.write(plainText("cancelled"))

                failedProvider.write(plainText("trigger failure"))
                assertTrue(assertFailsWith<IllegalStateException> { failedProvider.join() } === expectedFailure)
                failedProvider.write(plainText("failed"))

                assertEquals(listOf("provider_closed"), closedCollector.errorTypes)
                assertEquals(listOf("provider_closed"), cancelledCollector.errorTypes)
                assertEquals(listOf("provider_failed"), failedCollector.errorTypes)
            } finally {
                runCatching { closedProvider.forceShutdown() }
                runCatching { cancelledProvider.forceShutdown() }
                runCatching { failedProvider.forceShutdown() }
            }
        }

    @Test
    fun failed_channel_close_cause_is_classified_as_provider_failed() =
        runBlocking {
            val firstEntered = CompletableDeferred<Unit>()
            val releaseFirst = CompletableDeferred<Unit>()
            val expectedFailure = IllegalStateException("channel failed")
            val collector = RecordingMetricsCollector()
            val provider =
                object : Provider(ConsoleProviderConfig()) {
                    override suspend fun onMessage(record: LogRecord) {
                        firstEntered.complete(Unit)
                        releaseFirst.await()
                    }
                }.also { it.effectiveMetricsCollector = collector }

            try {
                provider.write(plainText("blocking"))
                firstEntered.await()
                provider.channel.close(expectedFailure)

                provider.write(plainText("rejected"))

                assertEquals(listOf("provider_failed"), collector.errorTypes)
                releaseFirst.complete(Unit)
                val joinFailure = assertFailsWith<IllegalStateException> { provider.join() }
                assertEquals(expectedFailure.message, joinFailure.message)
            } finally {
                releaseFirst.complete(Unit)
                runCatching { provider.forceShutdown() }
            }
        }

    @Test
    fun filtered_and_metrics_records_do_not_report_rejection_errors() =
        runBlocking {
            val collector = RecordingMetricsCollector()
            val provider =
                recordingProvider(
                    collector = collector,
                    config = ConsoleProviderConfig().apply { level = LogLevel.INFO }
                )

            try {
                provider.close()
                provider.write(plainText("filtered", LogLevel.DEBUG))
                provider.write(
                    LogRecord.Metrics("metrics", "rejected metrics", LogLevel.ERROR, emptyMap())
                )
                provider.join()

                assertTrue(collector.errorTypes.isEmpty())
                assertEquals(0, collector.logCount)
            } finally {
                runCatching { provider.forceShutdown() }
            }
        }

    @Test
    fun rejection_metric_collector_failure_is_isolated() =
        runBlocking {
            val provider =
                recordingProvider().also {
                    it.effectiveMetricsCollector = ThrowingMetricsCollector
                }

            try {
                provider.close()
                provider.write(plainText("rejected"))
                provider.join()
                assertTrue(provider.job.isCompleted)
            } finally {
                runCatching { provider.forceShutdown() }
            }
        }

    private fun recordingProvider(
        collector: MetricsCollector? = null,
        config: ConsoleProviderConfig = ConsoleProviderConfig()
    ): Provider =
        object : Provider(config) {
            override suspend fun onMessage(record: LogRecord) = Unit
        }.also { provider ->
            if (collector != null) provider.effectiveMetricsCollector = collector
        }

    private fun plainText(
        message: String,
        level: Level = LogLevel.INFO
    ) = LogRecord.PlainText("test", message, level, emptyMap())
}