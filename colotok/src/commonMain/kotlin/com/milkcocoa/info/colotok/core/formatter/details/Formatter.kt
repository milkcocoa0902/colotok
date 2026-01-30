package com.milkcocoa.info.colotok.core.formatter.details

import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.serialization.KSerializer

interface Formatter {
    fun format(record: LogRecord.PlainText): String {
        return format(record.msg, record.level, record.attr)
    }

    fun <T : LogStructure> format(record: LogRecord.StructuredText<T>): String {
        return format(record.msg, record.serializer, record.level, record.attr)
    }
    /**
     * used for formatting message
     * @param msg[String] message
     * @param level[Level] log level
     * @return [String] formatted message
     */
    fun format(
        msg: String,
        level: Level
    ): String {
        TODO("Not Implemented")
    }

    /**
     * used for formatting message which structured
     * @param msg[LogStructure] message data object which extend from [LogStructure]. which annotated @kotlinx.Serializable
     * @param serializer[KSerializer] serializer to serialize [msg]
     * @param level[Level] log level
     * @return [String] formatted message
     */
    fun <T : LogStructure> format(
        msg: T,
        serializer: KSerializer<T>,
        level: Level
    ): String {
        TODO("Not Implemented")
    }

    /**
     * used for formatting message which structured
     * @param msg[LogStructure] message data object which extend from [LogStructure]. which annotated @kotlinx.Serializable
     * @param serializer[KSerializer] serializer to serialize [msg]
     * @param level[Level] log level
     * @return [String] formatted message
     */
    fun <T : LogStructure> format(
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attrs: Map<String, String>
    ): String {
        TODO("Not Implemented")
    }

    /**
     * used for formatting message which structured
     * @param msg[LogStructure] message data object which extend from [LogStructure]. which annotated @kotlinx.Serializable
     * @param serializer[KSerializer] serializer to serialize [msg]
     * @param level[Level] log level
     * @param attrs[Map] additional attributes
     * @return [String] formatted message
     */
    fun format(
        msg: String,
        level: Level,
        attrs: Map<String, String>
    ): String {
        TODO("Not Implemented")
    }
}