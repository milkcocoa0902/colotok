package com.milkcocoa.info.colotok.core.provider.cloudwatch

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Instant

class CloudwatchProviderTest {
    @Test
    fun `invalid configuration does not create a client`() {
        val factory = RecordingFactory()
        assertFailsWith<IllegalStateException> {
            CloudwatchProvider(validConfig(factory).apply { logGroup = " " })
        }
        assertEquals(0, factory.createCount)
    }

    @Test
    fun `unused provider does not initialize client during graceful shutdown`() =
        runTest {
            val factory = RecordingFactory()
            val provider = CloudwatchProvider(validConfig(factory))

            try {
                provider.close()
                provider.join()

                assertEquals(0, factory.createCount)
                assertEquals(0, factory.client.closeCount)
            } finally {
                provider.forceShutdown()
            }
        }

    @Test
    fun `unused force shutdown does not initialize client`() {
        val factory = RecordingFactory()
        val provider = CloudwatchProvider(validConfig(factory))

        try {
            provider.forceShutdown()

            assertEquals(0, factory.createCount)
            assertEquals(0, factory.client.closeCount)
        } finally {
            provider.forceShutdown()
        }
    }

    @Test
    fun `publish uses event time and stable chronological order`() =
        runTest {
            val factory = RecordingFactory()
            val provider = CloudwatchProvider(validConfig(factory))
            val later = plain("later", Instant.fromEpochMilliseconds(2_000))
            val firstAtSameTime = plain("first-same", Instant.fromEpochMilliseconds(1_000))
            val secondAtSameTime = plain("second-same", Instant.fromEpochMilliseconds(1_000))

            try {
                provider.onPublish(listOf(later, firstAtSameTime, secondAtSameTime))
                provider.forceShutdown()

                val events = factory.client.calls.single()
                assertEquals(listOf(1_000L, 1_000L, 2_000L), events.map { it.timestampMillis })
                assertTrue(events[0].message.contains("first-same"))
                assertTrue(events[1].message.contains("second-same"))
                assertEquals(1, factory.client.closeCount)
            } finally {
                provider.forceShutdown()
            }
        }

    @Test
    fun `a failed later batch remains visible and stops subsequent publishing`() =
        runTest {
            val factory = RecordingFactory().apply { client.failOnPut = 2 }
            val provider = CloudwatchProvider(validConfig(factory))
            val records =
                listOf(
                    plain("first", Instant.fromEpochMilliseconds(0)),
                    plain("second", Instant.fromEpochMilliseconds(CLOUDWATCH_MAX_BATCH_SPAN_MILLIS + 1))
                )

            try {
                assertFailsWith<TestPublishException> { provider.onPublish(records) }
                assertEquals(2, factory.client.calls.size)
            } finally {
                provider.forceShutdown()
            }
            assertEquals(1, factory.client.closeCount)
        }

    @Test
    fun `partial batch failure retains the original records and may duplicate an earlier batch`() =
        runTest {
            val factory = RecordingFactory().apply { client.failOnPut = 2 }
            val provider = CloudwatchProvider(validConfig(factory))
            val first = plain("first", Instant.fromEpochMilliseconds(0))
            val second = plain("second", Instant.fromEpochMilliseconds(CLOUDWATCH_MAX_BATCH_SPAN_MILLIS + 1))

            try {
                provider.write(first)
                provider.write(second)
                provider.flush()

                assertEquals(4, factory.client.calls.size)
                assertTrue(factory.client.calls[0].single().message.contains("first"))
                assertTrue(factory.client.calls[1].single().message.contains("second"))
                assertTrue(factory.client.calls[2].single().message.contains("first"))
                assertTrue(factory.client.calls[3].single().message.contains("second"))
            } finally {
                provider.forceShutdown()
            }
            assertEquals(1, factory.client.closeCount)
        }

    @Test
    fun `provisioning failure is propagated and used client closes once gracefully`() =
        runTest {
            val expected = TestPublishException()
            val factory = RecordingFactory().apply { client.ensureFailure = expected }
            val provider = CloudwatchProvider(validConfig(factory))

            try {
                val actual =
                    assertFailsWith<TestPublishException> {
                        provider.onPublish(listOf(plain("message", Instant.fromEpochMilliseconds(1_000))))
                    }
                assertTrue(actual === expected)
                provider.close()
                provider.join()
                assertEquals(1, factory.client.closeCount)
            } finally {
                provider.forceShutdown()
            }
        }

    private fun validConfig(factory: CloudwatchClientFactory? = null) =
        CloudwatchProviderConfig().apply {
            logGroup = "test-group"
            logStream = "test-stream"
            credential = CloudwatchCredential.Default("ap-northeast-1")
            bufferSize = 2
            if (factory != null) clientFactory = factory
        }

    private fun plain(
        message: String,
        timestamp: Instant
    ) = LogRecord.PlainText(
        name = "test",
        msg = message,
        level = LogLevel.INFO,
        attr = emptyMap(),
        eventTimestamp = timestamp
    )

    private class RecordingFactory : CloudwatchClientFactory {
        var createCount = 0
        val client = RecordingClient()

        override fun create(credential: CloudwatchCredential): CloudwatchClient {
            createCount++
            return client
        }
    }

    private class RecordingClient : CloudwatchClient {
        val calls = mutableListOf<List<CloudwatchEvent>>()
        var failOnPut: Int? = null
        var ensureFailure: RuntimeException? = null
        var closeCount = 0

        override suspend fun ensureLogGroup(logGroup: String) {
            ensureFailure?.let { throw it }
        }

        override suspend fun ensureLogStream(
            logGroup: String,
            logStream: String
        ) = Unit

        override suspend fun putLogEvents(
            logGroup: String,
            logStream: String,
            events: List<CloudwatchEvent>,
            sequenceToken: String?
        ): String? {
            calls += events
            if (calls.size == failOnPut) throw TestPublishException()
            return "token-${calls.size}"
        }

        override fun close() {
            closeCount++
        }
    }

    private class TestPublishException : RuntimeException("publish failed")
}