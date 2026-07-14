package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.util.ThreadWrapper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.KSerializer
import kotlin.time.Clock
import kotlin.time.Instant

internal class LogEventMetadata private constructor(
    val attr: Map<String, String>,
    val threadName: String,
    val caller: String,
    private val mdcContextData: MDCContextData
) {
    fun mdcContextDataCopy(): MDCContextData = mdcContextData.deepCopy()

    companion object {
        fun capture(attr: Map<String, String>) = LogEventMetadata(
            attr = attr.toMap(),
            threadName = ThreadWrapper.getCurrentThreadName(),
            caller = ThreadWrapper.traceCallPoint(),
            mdcContextData = MDC.getThreadLocalContext().deepCopy()
        )
    }
}

sealed interface LogRecord{
    val name: String
    val level: Level
    val attr: Map<String, String>
    val threadName: String
    val mdcContextDataSnapshot: MDCContextData

    // Formatter を受け取って、自分自身をフォーマットさせる
    fun format(formatter: Formatter): String

    data class PlainText(
        override val name: String,
        val msg: String,
        override val level: Level,
        override val attr: Map<String, String>,
        val eventTimestamp: Instant = Clock.System.now(),
    ): LogRecord{
        internal val eventMetadata = LogEventMetadata.capture(attr)
        override val threadName: String get() = eventMetadata.threadName
        override val mdcContextDataSnapshot: MDCContextData get() = eventMetadata.mdcContextDataCopy()
        override fun format(formatter: Formatter): String = formatter.format(this)
    }

    data class StructuredText<T : LogStructure>(
        override val name: String,
        val msg: T,
        override val level: Level,
        override val attr: Map<String, String>,
        val serializer: KSerializer<T>,
        val eventTimestamp: Instant = Clock.System.now(),
    ): LogRecord{
        internal val eventMetadata = LogEventMetadata.capture(attr)
        override val threadName: String get() = eventMetadata.threadName
        override val mdcContextDataSnapshot: MDCContextData get() = eventMetadata.mdcContextDataCopy()
        override fun format(formatter: Formatter): String = formatter.format(this)
    }

    data class Metrics(
        override val name: String,
        val msg: String,
        override val level: Level,
        override val attr: Map<String, String>,
        val eventTimestamp: Instant = Clock.System.now(),
    ): LogRecord {
        internal val eventMetadata = LogEventMetadata.capture(attr)
        override val threadName: String get() = eventMetadata.threadName
        override val mdcContextDataSnapshot: MDCContextData get() = eventMetadata.mdcContextDataCopy()
        override fun format(formatter: Formatter): String = formatter.format(this)
    }

    data class Pin(
        val deferred: CompletableDeferred<Unit>
    ): LogRecord {
        override val name: String = "Pin"
        override val level: Level = LogLevel.OFF
        override val attr: Map<String, String> = emptyMap()
        override val threadName: String = ""
        override val mdcContextDataSnapshot: MDCContextData = MDCContextData()

        override fun format(formatter: Formatter): String = formatter.format(LogRecord.PlainText(name, "Pin", level, attr))
    }
}

internal val LogRecord.eventAttrSnapshot: Map<String, String>
    get() = when (this) {
        is LogRecord.PlainText -> eventMetadata.attr
        is LogRecord.StructuredText<*> -> eventMetadata.attr
        is LogRecord.Metrics -> eventMetadata.attr
        is LogRecord.Pin -> attr
    }

internal val LogRecord.eventCallerSnapshot: String
    get() = when (this) {
        is LogRecord.PlainText -> eventMetadata.caller
        is LogRecord.StructuredText<*> -> eventMetadata.caller
        is LogRecord.Metrics -> eventMetadata.caller
        is LogRecord.Pin -> ""
    }
