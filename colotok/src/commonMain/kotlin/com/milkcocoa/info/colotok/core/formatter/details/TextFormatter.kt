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
        val localDateTime: LocalDateTime by lazy { timestamp.toLocalDateTime(TimeZone.UTC) }

        return fmt.replace(DateTimeElementRegex){
            localDateTime.format(LocalDateTime.Formats.ISO)
        }.replace(DateElementRegex){
            localDateTime.date.format(LocalDate.Formats.ISO)
        }.replace(TimeElementRegex){
            localDateTime.time.format(LocalTime.Formats.ISO)
        }.replace(MessageElementRegex){
            msg
        }.replace(LevelElementRegex){
            level.toString()
        }.replace(ThreadElementRegex){
            threadName
        }.replace(AttributesElementRegex){
            attrs.toString()
        }.replace(CallerElementRegex){
            caller
        }.let {
            mdc.keys.fold(it) { acc, k -> acc.replace(Element.CUSTOM(k).toString(), mdc.get(k).toString()) }
        }
    }

    companion object {
        private val DateTimeElementRegex by lazy { Regex.fromLiteral(Element.DATETIME.toString()) }
        private val DateElementRegex by lazy { Regex.fromLiteral(Element.DATE.toString()) }
        private val TimeElementRegex by lazy { Regex.fromLiteral(Element.TIME.toString()) }
        private val MessageElementRegex by lazy { Regex.fromLiteral(Element.MESSAGE.toString()) }
        private val LevelElementRegex by lazy { Regex.fromLiteral(Element.LEVEL.toString()) }
        private val ThreadElementRegex by lazy { Regex.fromLiteral(Element.THREAD.toString()) }
        private val AttributesElementRegex by lazy { Regex.fromLiteral(Element.ATTR.toString()) }
        private val CallerElementRegex by lazy { Regex.fromLiteral(Element.CALLER.toString()) }
    }
}