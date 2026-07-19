package com.milkcocoa.info.colotok.core.formatter.details

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.logger.eventAttrSnapshot
import com.milkcocoa.info.colotok.core.logger.eventCallerSnapshot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * base class for log formatter which used for text log.
 *
 * this formatter formats [LogRecord] into plain text.
 * if you use structured logging with this formatter, it uses [LogStructure.stringify] as a message.
 *
 * @constructor
 * @param fmt[String] format string.
 *
 * you can use following placeholders:
 * - [Element.DATETIME] : date time(yyyy-MM-dd'T'HH:mm:ss.SSS+offset)
 * - [Element.DATE] : date(yyyy-MM-dd)
 * - [Element.TIME] : time(HH:mm:ss)
 * - [Element.LEVEL] : log level
 * - [Element.MESSAGE] : log message
 * - [Element.THREAD] : logging thread
 * - [Element.ATTR] : additional attributes
 * - [Element.CALLER] : caller point
 * - [Element.CUSTOM] : MDC values
 */
abstract class TextFormatter(private val fmt: String) : Formatter {
    override fun format(record: LogRecord.PlainText): String {
        return format(
            msg = record.msg,
            level = record.level,
            attrs = record.eventAttrSnapshot,
            threadName = record.threadName,
            mdc = record.mdcContextDataSnapshot.data,
            caller = record.eventCallerSnapshot,
            timestamp = record.eventTimestamp
        )
    }

    override fun <T : LogStructure> format(record: LogRecord.StructuredText<T>): String {
        return format(
            msg = record.msg.stringify(),
            level = record.level,
            attrs = record.eventAttrSnapshot,
            threadName = record.threadName,
            mdc = record.mdcContextDataSnapshot.data,
            caller = record.eventCallerSnapshot,
            timestamp = record.eventTimestamp
        )
    }

    override fun format(record: LogRecord.Metrics): String {
        return format(
            msg = record.msg,
            level = record.level,
            attrs = record.eventAttrSnapshot,
            threadName = record.threadName,
            mdc = record.mdcContextDataSnapshot.data,
            caller = record.eventCallerSnapshot,
            timestamp = record.eventTimestamp
        )
    }

    private fun format(
        msg: String,
        level: Level,
        attrs: Map<String, String>,
        threadName: String,
        mdc: Map<String, Any?>,
        caller: String,
        timestamp: Instant
    ): String {
        val dt = timestamp
        return fmt
            .replace(Element.DATETIME.toString(), dt.toLocalDateTime(TimeZone.UTC).format(LocalDateTime.Formats.ISO))
            .replace(Element.DATE.toString(), dt.toLocalDateTime(TimeZone.UTC).date.format(LocalDate.Formats.ISO))
            .replace(Element.TIME.toString(), dt.toLocalDateTime(TimeZone.UTC).time.format(LocalTime.Formats.ISO))
            .replace(Element.MESSAGE.toString(), msg)
            .replace(Element.LEVEL.toString(), level.toString())
            .replace(Element.THREAD.toString(), threadName)
            .replace(Element.ATTR.toString(), attrs.toString())
            .replace(Element.CALLER.toString(), caller)
            .let {
                mdc.keys.fold(it) { acc, k -> acc.replace(Element.CUSTOM(k).toString(), mdc.get(k).toString()) }
            }
    }
}