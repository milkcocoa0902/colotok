package com.milkcocoa.info.colotok.core.provider.loki

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class LokiPushPayloadTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun testLokiStreamSerialization() {
        // Create a LokiStream with specific values
        val timestamp1 = Instant.fromEpochSeconds(1721224800, 0) // 2024-07-17T12:00:00Z
        val timestamp2 = Instant.fromEpochSeconds(1721224801, 0) // 2024-07-17T12:00:01Z
        val stream = mapOf("app" to "test-app", "level" to "info")
        val values = listOf(
            LokiValue(timestamp1, "log message 1"),
            LokiValue(timestamp2, "log message 2")
        )

        val lokiStream = LokiStream(stream, values)

        // Serialize to JSON
        val json = Json.encodeToString(LokiStream.serializer(), lokiStream)

        // Expected JSON structure (actual string comparison would be brittle due to ordering)
        val deserializedStream = Json.decodeFromString(LokiStream.serializer(), json)

        // Verify the deserialized stream matches the original
        assertEquals(stream, deserializedStream.stream)
        assertEquals(2, deserializedStream.values.size)
        assertEquals("log message 1", deserializedStream.values[0].value)
        assertEquals("log message 2", deserializedStream.values[1].value)
        assertEquals(timestamp1, deserializedStream.values[0].timestamp)
        assertEquals(timestamp2, deserializedStream.values[1].timestamp)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testLokiPushPayloadSerialization() {
        // Create a LokiPushPayload with specific values
        val timestamp = Instant.fromEpochSeconds(1721224800, 0) // 2024-07-17T12:00:00Z
        val stream1 = mapOf("app" to "test-app", "level" to "info")
        val values1 = listOf(
            LokiValue(timestamp, "info log message")
        )

        val stream2 = mapOf("app" to "test-app", "level" to "error")
        val values2 = listOf(
            LokiValue(timestamp, "error log message")
        )

        val lokiStreams = listOf(
            LokiStream(stream1, values1),
            LokiStream(stream2, values2)
        )

        val lokiPushPayload = LokiPushPayload(lokiStreams)

        // Serialize to JSON
        val json = Json.encodeToString(LokiPushPayload.serializer(), lokiPushPayload)

        // Deserialize back to LokiPushPayload
        val deserializedPayload = Json.decodeFromString(LokiPushPayload.serializer(), json)

        // Verify the deserialized payload matches the original
        assertEquals(2, deserializedPayload.streams.size)

        // Check first stream
        assertEquals(stream1, deserializedPayload.streams[0].stream)
        assertEquals(1, deserializedPayload.streams[0].values.size)
        assertEquals("info log message", deserializedPayload.streams[0].values[0].value)

        // Check second stream
        assertEquals(stream2, deserializedPayload.streams[1].stream)
        assertEquals(1, deserializedPayload.streams[1].values.size)
        assertEquals("error log message", deserializedPayload.streams[1].values[0].value)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testLokiPushPayloadDeserialization() {
        // JSON representing a LokiPushPayload
        val json = """
            {
                "streams": [
                    {
                        "stream": {
                            "app": "test-app",
                            "level": "info"
                        },
                        "values": [
                            ["1721224800000000000", "info log message"]
                        ]
                    }
                ]
            }
        """.trimIndent()

        // Deserialize to LokiPushPayload
        val lokiPushPayload = Json.decodeFromString(LokiPushPayload.serializer(), json)

        // Expected values
        val expectedTimestamp = Instant.fromEpochSeconds(1721224800, 0)
        val expectedStream = mapOf("app" to "test-app", "level" to "info")
        val expectedValue = "info log message"

        // Verify the deserialized payload has the expected properties
        assertEquals(1, lokiPushPayload.streams.size)
        assertEquals(expectedStream, lokiPushPayload.streams[0].stream)
        assertEquals(1, lokiPushPayload.streams[0].values.size)
        assertEquals(expectedTimestamp, lokiPushPayload.streams[0].values[0].timestamp)
        assertEquals(expectedValue, lokiPushPayload.streams[0].values[0].value)
    }
}
