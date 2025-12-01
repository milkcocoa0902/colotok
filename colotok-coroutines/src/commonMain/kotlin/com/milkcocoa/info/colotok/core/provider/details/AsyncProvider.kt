package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.coroutines.blocking
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import kotlinx.serialization.KSerializer
import kotlin.text.String

/**
 * An interface for providers that support both synchronous and asynchronous logging operations.
 * 
 * This interface extends Provider and implements the synchronous `write` methods by calling
 * the corresponding asynchronous `writeAsync` methods wrapped in the `blocking` function.
 * 
 * Note that on Kotlin/JS platform, the synchronous methods will not actually block due to
 * platform limitations, and will return immediately while the logging happens asynchronously.
 */
interface AsyncProvider: Provider {
    /**
     * Asynchronous write operation for plain text data.
     * As a default implementation, this calls the overloaded version with an empty attribute map.
     * 
     * @param name[String] logger name
     * @param msg[String] data to write out.
     * @param level[Level] log level
     */
    suspend fun writeAsync(
        name: String,
        msg: String,
        level: Level
    ) {
        writeAsync(name, msg, level, mapOf())
    }

    /**
     * Asynchronous write operation for plain text with additional data.
     * This is the primary method that concrete implementations must override.
     * 
     * @param name[String] logger name
     * @param msg[String] data to write out.
     * @param level[Level] log level
     * @param attr[Map] additional attributes
     */
    suspend fun writeAsync(
        name: String,
        msg: String,
        level: Level,
        attr: Map<String, String>
    )

    /**
     * Asynchronous write operation for serializable data.
     * As a default implementation, this calls the overloaded version with an empty attribute map.
     * 
     * @param name[String] logger name
     * @param msg[LogStructure] data to write out.
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     * @param level[Level] log level
     */
    suspend fun <T : LogStructure> writeAsync(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level
    ) {
        writeAsync(name, msg, serializer, level, mapOf())
    }

    /**
     * Asynchronous write operation for serializable data with additional attributes.
     * This is the primary method that concrete implementations must override for structured logging.
     * 
     * @param name[String] logger name
     * @param msg[LogStructure] data to write out.
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     * @param level[Level] log level
     * @param attr[Map] additional attributes
     */
    suspend fun <T : LogStructure> writeAsync(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attr: Map<String, String>
    )

    /**
     * Synchronous implementation that calls the asynchronous writeAsync method.
     * 
     * This method uses the `blocking` function which has different behaviors across platforms:
     * - On JVM, Android, and Native: Truly blocks until the async operation completes
     * - On JS: Returns immediately while the async operation continues in the background
     */
    override fun write(name: String, msg: String, level: Level, attr: Map<String, String>) {
        blocking { writeAsync(name, msg, level, attr) }
    }

    /**
     * Synchronous implementation that calls the asynchronous writeAsync method.
     * 
     * This method uses the `blocking` function which has different behaviors across platforms:
     * - On JVM, Android, and Native: Truly blocks until the async operation completes
     * - On JS: Returns immediately while the async operation continues in the background
     */
    override fun write(name: String, msg: String, level: Level) {
        blocking { writeAsync(name, msg, level) }
    }

    /**
     * Synchronous implementation that calls the asynchronous writeAsync method.
     * 
     * This method uses the `blocking` function which has different behaviors across platforms:
     * - On JVM, Android, and Native: Truly blocks until the async operation completes
     * - On JS: Returns immediately while the async operation continues in the background
     */
    override fun <T : LogStructure> write(name: String, msg: T, serializer: KSerializer<T>, level: Level) {
        blocking { writeAsync(name, msg, serializer, level) }
    }

    /**
     * Synchronous implementation that calls the asynchronous writeAsync method.
     * 
     * This method uses the `blocking` function which has different behaviors across platforms:
     * - On JVM, Android, and Native: Truly blocks until the async operation completes
     * - On JS: Returns immediately while the async operation continues in the background
     */
    override fun <T : LogStructure> write(name: String, msg: T, serializer: KSerializer<T>, level: Level, attr: Map<String, String>) {
        blocking { writeAsync(name, msg, serializer, level, attr) }
    }
}
