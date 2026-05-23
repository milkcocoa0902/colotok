package com.milkcocoa.info.colotok.core.formatter.details

import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.serialization.KSerializer

/**
 * Interface for log formatter.
 *
 * This interface provides methods for formatting [LogRecord] into [String].
 */
interface Formatter {
    /**
     * used for formatting [LogRecord]
     * @param record [LogRecord]
     * @return [String] formatted message
     */
    fun format(record: LogRecord): String {
        return when (record) {
            is LogRecord.PlainText -> format(record)
            is LogRecord.StructuredText<*> -> format(record)
            is LogRecord.Metrics -> format(record)
            is LogRecord.Pin -> format(record)
        }
    }

    /**
     * used for formatting [LogRecord.PlainText]
     * @param record [LogRecord.PlainText]
     * @return [String] formatted message
     */
    fun format(record: LogRecord.PlainText): String

    /**
     * used for formatting [LogRecord.StructuredText]
     * @param record [LogRecord.StructuredText]
     * @return [String] formatted message
     */
    fun <T : LogStructure> format(record: LogRecord.StructuredText<T>): String

    /**
     * used for formatting [LogRecord.Metrics]
     * @param record [LogRecord.Metrics]
     * @return [String] formatted message
     */
    fun format(record: LogRecord.Metrics): String

    /**
     * used for formatting [LogRecord.Pin]
     * @param record [LogRecord.Pin]
     * @return [String] formatted message
     */
    fun format(record: LogRecord.Pin): String {
        return format(LogRecord.PlainText(record.name, "Pin", record.level, record.attr))
    }
}