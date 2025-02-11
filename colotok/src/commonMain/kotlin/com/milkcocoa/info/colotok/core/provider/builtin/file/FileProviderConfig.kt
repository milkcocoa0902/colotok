package com.milkcocoa.info.colotok.core.provider.builtin.file

import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.ProviderConfig
import com.milkcocoa.info.colotok.core.provider.rotation.Rotation
import com.milkcocoa.info.colotok.util.unit.Size.KiB

class FileProviderConfig() : ProviderConfig {
    /**
     * size of buffer in Byte. which is used for save i/o.
     */
    var bufferSize = 4.KiB()

    /**
     * use buffering. if enable this, provider does not write to file until buffered data exceed `bufferSize`. if false write data into file immediately.
     */
    var enableBuffer = true

    /**
     * if not null, rotate after write
     */
    var rotation: Rotation? = null

    override var level: Level = LogLevel.DEBUG

    override var formatter: Formatter = DetailTextFormatter
}