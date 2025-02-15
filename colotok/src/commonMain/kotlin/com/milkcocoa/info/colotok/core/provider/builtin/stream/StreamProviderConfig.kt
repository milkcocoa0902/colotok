package com.milkcocoa.info.colotok.core.provider.builtin.stream

import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.ProviderConfig
import com.milkcocoa.info.colotok.util.unit.Size.KiB
import okio.BufferedSink
import okio.Sink
import okio.blackholeSink
import okio.buffer

class StreamProviderConfig : ProviderConfig {
    override var level: Level = LogLevel.DEBUG
    override var formatter: Formatter = DetailTextFormatter

    /**
     * size of buffer in Byte. which is used for save i/o.
     */
    var bufferSize = 4.KiB()

    /**
     * use buffering. if enable this, provider does not write to file until buffered data exceed `bufferSize`. if false write data into file immediately.
     */
    var enableBuffer = true

    var outputStreamBuilder: (() -> Sink) = { blackholeSink().buffer() }
}