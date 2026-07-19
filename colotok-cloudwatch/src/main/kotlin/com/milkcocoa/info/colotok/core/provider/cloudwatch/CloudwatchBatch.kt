package com.milkcocoa.info.colotok.core.provider.cloudwatch

internal const val CLOUDWATCH_MAX_BATCH_BYTES = 1_048_576L
internal const val CLOUDWATCH_MAX_BATCH_EVENTS = 10_000
internal const val CLOUDWATCH_MAX_BATCH_SPAN_MILLIS = 24L * 60 * 60 * 1_000
private const val CLOUDWATCH_EVENT_OVERHEAD_BYTES = 26L

internal data class CloudwatchEvent(
    val timestampMillis: Long,
    val message: String,
)

internal data class IndexedCloudwatchEvent(
    val index: Int,
    val event: CloudwatchEvent,
)

internal fun partitionCloudwatchEvents(
    indexedEvents: List<IndexedCloudwatchEvent>,
): List<List<CloudwatchEvent>> {
    if (indexedEvents.isEmpty()) return emptyList()

    val sorted = indexedEvents.sortedWith(
        compareBy<IndexedCloudwatchEvent> { it.event.timestampMillis }.thenBy { it.index },
    )
    val batches = mutableListOf<List<CloudwatchEvent>>()
    var current = mutableListOf<CloudwatchEvent>()
    var currentBytes = 0L

    fun commitCurrent() {
        if (current.isNotEmpty()) batches += current
        current = mutableListOf()
        currentBytes = 0L
    }

    sorted.forEach { indexed ->
        val event = indexed.event
        val eventBytes = event.message.encodeToByteArray().size.toLong() + CLOUDWATCH_EVENT_OVERHEAD_BYTES
        require(eventBytes <= CLOUDWATCH_MAX_BATCH_BYTES) {
            "Cloudwatch event at input index ${indexed.index} is $eventBytes bytes; maximum is $CLOUDWATCH_MAX_BATCH_BYTES"
        }

        val exceedsCount = current.size == CLOUDWATCH_MAX_BATCH_EVENTS
        val exceedsBytes = currentBytes + eventBytes > CLOUDWATCH_MAX_BATCH_BYTES
        val firstTimestamp = current.firstOrNull()?.timestampMillis
        val exceedsSpan = firstTimestamp != null &&
            firstTimestamp <= Long.MAX_VALUE - CLOUDWATCH_MAX_BATCH_SPAN_MILLIS &&
            event.timestampMillis > firstTimestamp + CLOUDWATCH_MAX_BATCH_SPAN_MILLIS
        if (exceedsCount || exceedsBytes || exceedsSpan) commitCurrent()

        current += event
        currentBytes += eventBytes
    }
    commitCurrent()
    return batches
}
