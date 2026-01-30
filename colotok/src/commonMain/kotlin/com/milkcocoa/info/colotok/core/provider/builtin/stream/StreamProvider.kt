package com.milkcocoa.info.colotok.core.provider.builtin.stream

import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.Provider
import okio.buffer
import okio.use

class StreamProvider(config: StreamProviderConfig) : Provider() {
    constructor(config: StreamProviderConfig.() -> Unit) : this(StreamProviderConfig().apply(config))
    constructor() : this(StreamProviderConfig())

    private val logLevel = config.level
    private val formatter = config.formatter
    private val outputStream = config.outputStreamBuilder

    private val enableBuffer = config.enableBuffer
    private val bufferSize = config.bufferSize

    private val sb: StringBuilder = StringBuilder()

    override fun onMessage(record: LogRecord) {
        if(record.level.isEnabledFor(logLevel).not()) return

        runCatching {
            if(enableBuffer){
                sb.appendLine(record.format(formatter))
                if(sb.length > bufferSize){
                    outputStream.invoke().buffer().use { sink ->
                        sink.write(sb.toString().encodeToByteArray())
                        sink.flush()
                    }
                    sb.clear()
                }
            }else{
                outputStream.invoke().buffer().use { sink ->
                    sink.write(record.format(formatter).plus("\n").encodeToByteArray())
                    sink.flush()
                }
            }
        }
    }

    /**
     * flush buffered data into file.
     */
    override suspend fun onFlush() {
        outputStream.invoke().buffer().use { sink ->
            sink.write(sb.toString().encodeToByteArray())
            sink.flush()
        }
        sb.clear()
    }
}