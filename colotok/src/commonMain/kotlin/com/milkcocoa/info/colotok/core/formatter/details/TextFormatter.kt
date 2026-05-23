package com.milkcocoa.info.colotok.core.formatter.details

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.logger.MDC
import com.milkcocoa.info.colotok.util.ThreadWrapper
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer

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
            attrs = record.attr,
            threadName = record.threadName,
            mdc = record.mdcContextDataSnapshot.data
        )
    }

    override fun <T : LogStructure> format(record: LogRecord.StructuredText<T>): String {
        return format(
            msg = record.msg.stringify(),
            level = record.level,
            attrs = record.attr,
            threadName = record.threadName,
            mdc = record.mdcContextDataSnapshot.data
        )
    }

    override fun <T : LogStructure> format(
        msg: T,
        serializer: KSerializer<T>,
        level: Level
    ): String {
        return format(msg.stringify(), level, mapOf())
    }

    override fun <T : LogStructure> format(
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attrs: Map<String, String>
    ): String {
        return format(
            msg = msg.stringify(),
            level = level,
            attrs = attrs,
            threadName = ThreadWrapper.getCurrentThreadName(),
            mdc = MDC.getThreadLocalContext().data
        )
    }

    override fun format(
        msg: String,
        level: Level
    ): String {
        return format(msg, level, mapOf())
    }

    override fun format(
        msg: String,
        level: Level,
        attrs: Map<String, String>
    ): String {
        return format(
            msg = msg,
            level = level,
            attrs = attrs,
            threadName = ThreadWrapper.getCurrentThreadName(),
            mdc = MDC.getThreadLocalContext().data
        )
    }

    private fun format(
        msg: String,
        level: Level,
        attrs: Map<String, String>,
        threadName: String,
        mdc: Map<String, Any?>
    ): String {
        val dt = kotlin.time.Clock.System.now()
        return fmt
            .replace(Element.DATETIME.toString(), dt.toLocalDateTime(TimeZone.UTC).format(LocalDateTime.Formats.ISO))
            .replace(Element.DATE.toString(), dt.toLocalDateTime(TimeZone.UTC).date.format(LocalDate.Formats.ISO))
            .replace(Element.TIME.toString(), dt.toLocalDateTime(TimeZone.UTC).time.format(LocalTime.Formats.ISO))
            .replace(Element.MESSAGE.toString(), msg)
            .replace(Element.LEVEL.toString(), level.toString())
            .replace(Element.THREAD.toString(), threadName)
            .replace(Element.ATTR.toString(), attrs.toString())
            .replace(Element.CALLER.toString(), ThreadWrapper.traceCallPoint())
            .let {
                mdc.keys.fold(it) { acc, k -> acc.replace(Element.CUSTOM(k).toString(), mdc.get(k).toString()) }
            }
    }
}