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
     * if not null, rotate after write
     */
    var rotation: Rotation? = null

    override var level: Level = LogLevel.DEBUG

    override var formatter: Formatter = DetailTextFormatter
}