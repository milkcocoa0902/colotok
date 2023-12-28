package com.milkcocoa.info.colotok.core.formatter.details

import com.milkcocoa.info.colotok.core.logger.LogLevel
import kotlinx.serialization.KSerializer

interface Formatter {
    fun format(msg: String, level: LogLevel): String
    fun<T: LogStructure> format(msg: T, serializer: KSerializer<T>, level: LogLevel): String
}