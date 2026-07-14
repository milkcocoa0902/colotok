package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

final class LevelScopedColotokLogger(
    val name: String,
    val providers: List<Provider>,
    val attrs: Map<String, String>,
    val level: Level
) {

    fun print(msg: String) {
        val record = LogRecord.PlainText(
            name = name,
            msg = msg,
            level = level,
            attr = attrs.toMap()
        )
        providers.forEach {
            it.write(record)
        }
    }

    fun print(
        msg: String,
        attr: Map<String, String>
    ) {
        val record = LogRecord.PlainText(
            name = name,
            msg = msg,
            level = level,
            attr = attrs.plus(attr).toMap()
        )
        providers.forEach {
            it.write(record)
        }
    }

    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> print(msg: T) {
        val record = LogRecord.StructuredText(
            name = name,
            msg = msg,
            level = level,
            serializer = T::class.serializer(),
            attr = attrs.toMap()
        )
        providers.forEach {
            it.write(record)
        }
    }

    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> print(
        msg: T,
        attr: Map<String, String>
    ) {
        val record = LogRecord.StructuredText(
            name = name,
            msg = msg,
            level = level,
            serializer = T::class.serializer(),
            attr = attrs.plus(attr).toMap()
        )
        providers.forEach {
            it.write(record)
        }
    }
}
