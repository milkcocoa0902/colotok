package com.milkcocoa.info.colotok.core.formatter.details

import com.milkcocoa.info.colotok.core.logger.LogLevel
import kotlinx.serialization.KSerializer

interface Formatter {
    /**
     * used for formatting message
     * @param msg[String] message
     * @param level[LogLevel] log level
     * @return [String] formatted message
     */
    fun format(msg: String, level: LogLevel): String{
        TODO("Not Implemented")
    }

    /**
     * used for formatting message which structured
     * @param msg[LogStructure] message data object which extend from [LogStructure]. which annotated @kotlinx.Serializable
     * @param serializer[KSerializer] serializer to serialize [msg]
     * @param level[LogLevel] log level
     * @return [String] formatted message
     */
    fun<T: LogStructure> format(msg: T, serializer: KSerializer<T>, level: LogLevel): String{
        TODO("Not Implemented")
    }

    /**
     * used for formatting message which structured
     * @param msg[LogStructure] message data object which extend from [LogStructure]. which annotated @kotlinx.Serializable
     * @param serializer[KSerializer] serializer to serialize [msg]
     * @param level[LogLevel] log level
     * @return [String] formatted message
     */
    fun<T: LogStructure> format(msg: T, serializer: KSerializer<T>, level: LogLevel, attrs: Map<String, String>): String{
        TODO("Not Implemented")
    }

    fun format(msg: String, level: LogLevel, attrs: Map<String, String>): String{
        TODO("Not Implemented")
    }
}