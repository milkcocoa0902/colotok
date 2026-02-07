package com.milkcocoa.info.colotok.core.formatter.details

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.logger.MDC
import com.milkcocoa.info.colotok.util.ThreadWrapper
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime

/**
 * base class for log formatter which used for text log
 *
 * @constructor
 * @param fmt[String] format string
 */
abstract class TextFormatter(private val fmt: String) : Formatter {
    override fun format(record: LogRecord.PlainText): String {
        val mdc = record.mdcContextDataSnapshot.data

        val dt = Clock.System.now()
        return fmt
            .replace(Element.DATETIME.toString(), dt.toLocalDateTime(TimeZone.UTC).format(LocalDateTime.Formats.ISO))
            .replace(Element.DATE.toString(), dt.toLocalDateTime(TimeZone.UTC).date.format(LocalDate.Formats.ISO))
            .replace(Element.TIME.toString(), dt.toLocalDateTime(TimeZone.UTC).time.format(LocalTime.Formats.ISO))
            .replace(Element.MESSAGE.toString(), record.msg.toString())
            .replace(Element.LEVEL.toString(), record.level.toString())
            .replace(Element.THREAD.toString(), record.threadName)
            .replace(Element.ATTR.toString(), record.attr.toString())
            .replace(Element.CALLER.toString(), ThreadWrapper.traceCallPoint())
            .let {
                mdc.keys.fold(it) { acc, k -> acc.replace(Element.CUSTOM(k).toString(), mdc.get(k).toString()) }
            }
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
        val mdc = MDC.getThreadLocalContext().data

        val dt = Clock.System.now()
        return fmt
            .replace(Element.DATETIME.toString(), dt.toLocalDateTime(TimeZone.UTC).format(LocalDateTime.Formats.ISO))
            .replace(Element.DATE.toString(), dt.toLocalDateTime(TimeZone.UTC).date.format(LocalDate.Formats.ISO))
            .replace(Element.TIME.toString(), dt.toLocalDateTime(TimeZone.UTC).time.format(LocalTime.Formats.ISO))
            .replace(Element.MESSAGE.toString(), msg.toString())
            .replace(Element.LEVEL.toString(), level.toString())
            .replace(Element.THREAD.toString(), ThreadWrapper.getCurrentThreadName())
            .replace(Element.ATTR.toString(), attrs.toString())
            .replace(Element.CALLER.toString(), ThreadWrapper.traceCallPoint())
            .let {
                mdc.keys.fold(it) { acc, k -> acc.replace(Element.CUSTOM(k).toString(), mdc.get(k).toString()) }
            }
    }
}