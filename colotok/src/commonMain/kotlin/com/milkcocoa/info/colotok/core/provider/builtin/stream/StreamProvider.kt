package com.milkcocoa.info.colotok.core.provider.builtin.stream

import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.Provider
import okio.buffer
import okio.use

class StreamProvider(config: StreamProviderConfig) : Provider(config) {
    constructor(config: StreamProviderConfig.() -> Unit) : this(StreamProviderConfig().apply(config))
    constructor() : this(StreamProviderConfig())

    private val outputStream = config.outputStreamBuilder

    override suspend fun onMessage(record: LogRecord) {
        runCatching {
            outputStream.invoke().buffer().use { sink ->
                sink.write(record.format(config.formatter).plus("\n").encodeToByteArray())
                sink.flush()
            }
        }
    }

    /**
     * flush buffered data into file.
     */
    override suspend fun onFlush() {}
}