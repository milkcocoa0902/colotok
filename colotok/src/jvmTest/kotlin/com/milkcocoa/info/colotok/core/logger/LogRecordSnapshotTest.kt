package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.formatter.details.StructuredFormatter
import com.milkcocoa.info.colotok.core.formatter.details.TextFormatter
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Instant

class LogRecordSnapshotTest {
    @Serializable
    private data class SnapshotStructure(val value: String) : LogStructure

    private class RecordingProvider : Provider(ConsoleProviderConfig()) {
        val records = mutableListOf<LogRecord>()

        override suspend fun onMessage(record: LogRecord) {
            records += record
        }
    }

    private val providersToClose = mutableListOf<RecordingProvider>()

    private fun recordingProvider() = RecordingProvider().also(providersToClose::add)

    @AfterTest
    fun tearDown() {
        providersToClose.forEach(RecordingProvider::forceShutdown)
        providersToClose.clear()
        MDC.clear()
    }

    @Test
    fun one_log_call_shares_record_instance_across_providers() = runBlocking {
        val firstProvider = recordingProvider()
        val secondProvider = recordingProvider()
        val logger = ColotokLogger(
            name = "snapshot",
            config = ColotokConfig().apply {
                providers = listOf(firstProvider, secondProvider)
            }
        )

        logger.info("message")
        firstProvider.flush()
        secondProvider.flush()

        assertSame(firstProvider.records.single(), secondProvider.records.single())
    }

    @Test
    fun logger_snapshots_mutable_attrs_and_mdc_at_call_time() = runBlocking {
        val provider = recordingProvider()
        val attrs = mutableMapOf("attr" to "before")
        val config = ColotokConfig().apply {
            providers = listOf(provider)
            defaultAttrs = attrs
        }
        val logger = ColotokLogger("snapshot", config)
        MDC.put("request_id", "before")

        logger.info("message")
        attrs["attr"] = "after"
        MDC.put("request_id", "after")
        provider.flush()

        val record = provider.records.single()
        assertEquals("before", record.attr["attr"])
        assertEquals("before", record.mdcContextDataSnapshot.data["request_id"])
    }

    @Test
    fun level_scoped_logger_snapshots_mutable_attrs_at_call_time() = runBlocking {
        val provider = recordingProvider()
        val attrs = mutableMapOf("attr" to "before")
        val logger = LevelScopedColotokLogger(
            name = "snapshot",
            providers = listOf(provider),
            attrs = attrs,
            level = LogLevel.INFO
        )

        logger.print("message")
        attrs["attr"] = "after"
        provider.flush()

        assertEquals("before", provider.records.single().attr["attr"])
    }

    @Test
    fun mdc_snapshot_accessor_returns_defensive_copy() {
        MDC.put("request_id", "captured")
        val record = LogRecord.PlainText("snapshot", "message", LogLevel.INFO, emptyMap())

        record.mdcContextDataSnapshot.data["request_id"] = "mutated"

        assertEquals("captured", record.mdcContextDataSnapshot.data["request_id"])
    }

    @Test
    fun delayed_format_uses_record_attr_snapshot() {
        val attrs = mutableMapOf("attr" to "captured")
        val record = LogRecord.PlainText("snapshot", "message", LogLevel.INFO, attrs)
        attrs["attr"] = "mutated"

        val formatted = object : TextFormatter("${Element.ATTR}") {}.format(record)

        assertEquals("{attr=captured}", formatted)
    }

    @Test
    fun logger_captures_caller_before_delayed_format() = runBlocking {
        val provider = recordingProvider()
        val logger = ColotokLogger(
            name = "snapshot",
            config = ColotokConfig().apply { providers = listOf(provider) }
        )

        logger.info("message")
        provider.flush()
        val record = provider.records.single()
        val formatted = object : TextFormatter("${Element.CALLER}") {}.format(record)
        val structured = object : StructuredFormatter(listOf(Element.CALLER)) {}.format(record)

        assertTrue(formatted.contains("logger_captures_caller_before_delayed_format"), formatted)
        assertTrue(structured.contains("logger_captures_caller_before_delayed_format"), structured)
    }

    @Test
    fun structured_logger_captures_caller_despite_inline_overload() = runBlocking {
        val provider = recordingProvider()
        val logger = ColotokLogger(
            name = "snapshot",
            config = ColotokConfig().apply { providers = listOf(provider) }
        )

        logger.info(SnapshotStructure("message"))
        provider.flush()
        val formatted = object : TextFormatter("${Element.CALLER}") {}.format(provider.records.single())

        assertTrue(formatted.contains("structured_logger_captures_caller_despite_inline_overload"), formatted)
    }

    @Test
    fun text_and_structured_formatters_use_explicit_event_timestamp() {
        val timestamp = Instant.parse("2020-02-03T04:05:06.789Z")
        val record = LogRecord.PlainText(
            name = "snapshot",
            msg = "message",
            level = LogLevel.INFO,
            attr = emptyMap(),
            eventTimestamp = timestamp,
        )

        val text = object : TextFormatter("${Element.DATETIME}") {}.format(record)
        val structured = object : StructuredFormatter(listOf(Element.DATETIME)) {}.format(record)

        assertEquals("2020-02-03T04:05:06.789", text)
        assertTrue(structured.contains("2020-02-03T04:05:06.789"), structured)
    }
}
