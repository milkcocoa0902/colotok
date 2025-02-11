package com.milkcocoa.info.colotok.core.provider.builtin.console

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.serialization.KSerializer

public expect class ConsoleProvider(config: ConsoleProviderConfig) : Provider {
    val logLevel: Level
    val formatter: Formatter

    override fun write(
        name: String,
        msg: String,
        level: Level,
        attr: Map<String, String>
    )

    override fun <T : LogStructure> write(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attr: Map<String, String>
    )
}