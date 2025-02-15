package com.milkcocoa.info.colotok.core.provider.builtin.stream

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.serialization.KSerializer
import okio.buffer
import okio.use

class StreamProvider(config: StreamProviderConfig) : Provider {
    constructor(config: StreamProviderConfig.() -> Unit) : this(StreamProviderConfig().apply(config))
    constructor() : this(StreamProviderConfig())

    private val logLevel = config.level
    private val formatter = config.formatter
    private val outputStream = config.outputStreamBuilder

    private val enableBuffer = config.enableBuffer
    private val bufferSize = config.bufferSize

    private val sb: StringBuilder = StringBuilder()

    override fun write(
        name: String,
        msg: String,
        level: Level,
        attr: Map<String, String>
    ) {
        if (level.isEnabledFor(logLevel).not()) {
            return
        }

        runCatching {
            if (enableBuffer) {
                sb.appendLine(formatter.format(msg, level, attr))
                if (sb.length > bufferSize) {
                    outputStream.invoke().buffer().use { sink ->
                        sink.write(sb.toString().encodeToByteArray())
                        sink.flush()
                    }
                    sb.clear()
                }
            } else {
                outputStream.invoke().buffer().use { sink ->
                    sink.write(formatter.format(msg.plus("\n"), level, attr).encodeToByteArray())
                    sink.flush()
                }
            }
        }
    }

    override fun <T : LogStructure> write(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attr: Map<String, String>
    ) {
        if (level.isEnabledFor(logLevel).not()) {
            return
        }

        runCatching {
            if (enableBuffer) {
                sb.appendLine(formatter.format(msg, serializer, level, attr))
                if (sb.length > bufferSize) {
                    outputStream.invoke().buffer().use { sink ->
                        sink.write(sb.toString().encodeToByteArray())
                        sink.flush()
                    }
                    sb.clear()
                }
            } else {
                outputStream.invoke().buffer().use { sink ->
                    sink.write(formatter.format(msg, serializer, level, attr).plus("\n").encodeToByteArray())
                    sink.flush()
                }
            }
        }
    }

    /**
     * flush buffered data into file.
     */
    fun flush() {
        outputStream.invoke().buffer().use { sink ->
            sink.write(sb.toString().encodeToByteArray())
            sink.flush()
        }
        sb.clear()
    }
}