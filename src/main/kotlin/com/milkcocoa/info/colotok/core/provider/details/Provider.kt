package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.logger.LogLevel
import kotlinx.serialization.KSerializer

interface Provider {
    fun write(name: String, msg: String, level: LogLevel)
    fun<T: LogStructure> write(name: String, msg: T, serializer: KSerializer<T>, level: LogLevel)

}

