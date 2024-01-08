package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import kotlinx.serialization.KSerializer

interface Provider {
    /**
     * write out the plain text data into other space. as a default, call overload with empty map
     * @param name[String] logger name
     * @param msg[String] data to write out.
     * @param level[Level] log level
     */
    fun write(
        name: String,
        msg: String,
        level: Level
    ) {
        write(name, msg, level, mapOf())
    }

    /**
     * write out the plain text with additional data into other space.
     * @param name[String] logger name
     * @param msg[String] data to write out.
     * @param level[Level] log level
     * @param attr[Map] additional attributes
     */
    fun write(
        name: String,
        msg: String,
        level: Level,
        attr: Map<String, String>
    )

    /**
     * write out the serializable data into other space. as a default, call overload with empty map
     * @param name[String] logger name
     * @param msg[LogStructure] data to write out.
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     * @param level[Level] log level
     */
    fun <T : LogStructure> write(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level
    ) {
        write(name, msg, serializer, level, mapOf())
    }

    /**
     * write out the serializable data with additional attributes into other space.
     * @param name[String] logger name
     * @param msg[LogStructure] data to write out.
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     * @param level[Level] log level
     * @param attr[Map] additional attributes
     */
    fun <T : LogStructure> write(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attr: Map<String, String>
    )
}