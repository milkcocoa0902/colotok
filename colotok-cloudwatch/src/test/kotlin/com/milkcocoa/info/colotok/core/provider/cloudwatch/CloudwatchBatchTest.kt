package com.milkcocoa.info.colotok.core.provider.cloudwatch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CloudwatchBatchTest {
    @Test
    fun `partitions at event count boundary`() {
        val events = (0..CLOUDWATCH_MAX_BATCH_EVENTS).map { indexed(it, "x") }

        val batches = partitionCloudwatchEvents(events)

        assertEquals(listOf(CLOUDWATCH_MAX_BATCH_EVENTS, 1), batches.map { it.size })
    }

    @Test
    fun `measures UTF-8 bytes and partitions before byte overflow`() {
        val first = "a".repeat((CLOUDWATCH_MAX_BATCH_BYTES / 2 - 26).toInt())
        val second = "é".repeat((CLOUDWATCH_MAX_BATCH_BYTES / 4).toInt())

        val batches = partitionCloudwatchEvents(listOf(indexed(0, first), indexed(1, second)))

        assertEquals(listOf(1, 1), batches.map { it.size })
    }

    @Test
    fun `allows exactly 24 hours and splits at one millisecond more`() {
        val batches = partitionCloudwatchEvents(
            listOf(
                indexed(0, "first", timestamp = 0),
                indexed(1, "boundary", timestamp = CLOUDWATCH_MAX_BATCH_SPAN_MILLIS),
                indexed(2, "after", timestamp = CLOUDWATCH_MAX_BATCH_SPAN_MILLIS + 1),
            ),
        )

        assertEquals(listOf(2, 1), batches.map { it.size })
    }

    @Test
    fun `rejects an oversized single event`() {
        val messageBytes = CLOUDWATCH_MAX_BATCH_BYTES - 26 + 1

        assertFailsWith<IllegalArgumentException> {
            partitionCloudwatchEvents(listOf(indexed(0, "x".repeat(messageBytes.toInt()))))
        }
    }

    @Test
    fun `time span comparison does not overflow at long extremes`() {
        val batches = partitionCloudwatchEvents(
            listOf(
                indexed(0, "minimum", timestamp = Long.MIN_VALUE),
                indexed(1, "maximum", timestamp = Long.MAX_VALUE),
            ),
        )

        assertEquals(listOf(1, 1), batches.map { it.size })
    }

    private fun indexed(index: Int, message: String, timestamp: Long = index.toLong()) =
        IndexedCloudwatchEvent(index, CloudwatchEvent(timestamp, message))
}
