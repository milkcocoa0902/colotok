package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.KSerializer

sealed interface LogRecord{
    val name: String
    val level: Level
    val attr: Map<String, String>

    // Formatter を受け取って、自分自身をフォーマットさせる
    fun format(formatter: Formatter): String

    data class PlainText(
        override val name: String,
        val msg: String,
        override val level: Level,
        override val attr: Map<String, String>
    ): LogRecord{
        override fun format(formatter: Formatter): String = formatter.format(msg, level, attr)
    }

    data class StructuredText<T : LogStructure>(
        override val name: String,
        val msg: T,
        override val level: Level,
        override val attr: Map<String, String>,
        val serializer: KSerializer<T>
    ): LogRecord{
        override fun format(formatter: Formatter): String = formatter.format(msg, serializer, level, attr)
    }

    data class Pin(
        val deferred: CompletableDeferred<Unit>
    ): LogRecord {
        override val name: String = "Pin"
        override val level: Level = LogLevel.OFF
        override val attr: Map<String, String> = emptyMap()

        override fun format(formatter: Formatter): String = formatter.format("Pin", level, attr)
    }
}