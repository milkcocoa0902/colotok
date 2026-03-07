package com.milkcocoa.info.colotok.core.provider.builtin.stream

import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.ProviderConfig
import com.milkcocoa.info.colotok.util.unit.Size.KiB
import okio.Sink
import okio.blackholeSink
import okio.buffer

class StreamProviderConfig : ProviderConfig {
    override var level: Level = LogLevel.DEBUG
    override var formatter: Formatter = DetailTextFormatter

    var outputStreamBuilder: (() -> Sink) = { blackholeSink().buffer() }
}