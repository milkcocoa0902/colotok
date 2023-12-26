package com.milkcocoa.info.clk.core.formatter.details

import com.milkcocoa.info.clk.core.logger.LogLevel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

interface Formatter {
    fun format(msg: String, level: LogLevel): String
    fun<T: LogStructure> format(msg: T, serializer: KSerializer<T>, level: LogLevel): String
}