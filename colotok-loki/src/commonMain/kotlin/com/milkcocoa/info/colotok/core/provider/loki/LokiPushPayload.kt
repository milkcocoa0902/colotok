package com.milkcocoa.info.colotok.core.provider.loki

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Example of the JSON payload structure sent to Loki:
 * 
 * {
 *   "streams": [
 *     {
 *       "stream": {
 *         "app": "colotok",
 *         "level": "info"
 *       },
 *       "values": [
 *         ["1721224800000000000", "user logged in"],
 *         ["1721224801000000000", "user fetched profile"]
 *       ]
 *     }
 *   ]
 * }
 */
/**
 * Represents the main payload sent to Loki's API.
 * 
 * This class follows Loki's API format for pushing logs, which consists of
 * a list of streams, each containing labels and log entries.
 * 
 * @property streams List of log streams to send to Loki
 */
@Serializable
internal data class LokiPushPayload(
    val streams: List<LokiStream>,
)

/**
 * Represents a single log entry with a timestamp and value.
 * 
 * @property timestamp The time when the log entry was created
 * @property value The log message content
 */
@OptIn(ExperimentalTime::class)
@Serializable(with = LokiValueSerializer::class)
internal data class LokiValue(
    val timestamp: Instant = Clock.System.now(),
    val value: String,
)

/**
 * Custom serializer for LokiValue that formats it according to Loki's API requirements.
 * 
 * Loki expects log entries as arrays with two elements: [timestamp_nanoseconds, message]
 */
internal class LokiValueSerializer : KSerializer<LokiValue> {
    // Use a list of strings as the serialized format
    private val stringListSerializer = ListSerializer(String.serializer())

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = listSerialDescriptor<String>()

    /**
     * Serializes a LokiValue to a list of strings in the format expected by Loki.
     * 
     * @param encoder The encoder to write to
     * @param value The LokiValue to serialize
     */
    @OptIn(ExperimentalTime::class)
    override fun serialize(encoder: Encoder, value: LokiValue) {
        // Convert timestamp to nanoseconds (seconds * 10^9 + nanosecond adjustment)
        val timestampNanos = (value.timestamp.epochSeconds * 1_000_000_000 + 
                             value.timestamp.nanosecondsOfSecond).toString()

        // Encode as a list of two strings: [timestamp_string, value_string]
        val stringList = listOf(timestampNanos, value.value)
        stringListSerializer.serialize(encoder, stringList)
    }

    /**
     * Deserializes a list of strings into a LokiValue.
     * 
     * @param decoder The decoder to read from
     * @return The deserialized LokiValue
     */
    @OptIn(ExperimentalTime::class)
    override fun deserialize(decoder: Decoder): LokiValue {
        // Decode from a list of strings
        val stringList = stringListSerializer.deserialize(decoder)

        if (stringList.size < 2) {
            return LokiValue(Clock.System.now(), "")
        }

        // Parse timestamp from nanoseconds
        val timestampNanos = stringList[0].toLongOrNull() ?: 0L
        val seconds = timestampNanos / 1_000_000_000
        val nanos = (timestampNanos % 1_000_000_000).toInt()
        val timestamp = Instant.fromEpochSeconds(seconds, nanos)

        return LokiValue(
            timestamp = timestamp,
            value = stringList[1]
        )
    }
}

/**
 * Represents a stream of log entries in Loki.
 * 
 * A stream consists of a set of labels (key-value pairs) that identify the stream,
 * and a list of log values (entries) that belong to this stream.
 * 
 * @property stream Map of labels that identify this log stream
 * @property values List of log entries in this stream
 */
@Serializable
internal data class LokiStream(
    val stream: Map<String, String>,
    val values: List<LokiValue>
)
