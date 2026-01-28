package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

final class LevelScopedColotokLogger(
    val name: String,
    private val config: ColotokConfig,
    val level: Level
) {
    val providers get() = config.providers
    val attrs get() = config.defaultAttrs

    fun print(msg: String) {
        providers.forEach {
            it.write(LogRecord.PlainText(
                name = name,
                msg = msg,
                level = level,
                attr = attrs
            ))
        }
    }

    fun print(
        msg: String,
        attr: Map<String, String>
    ) {
        providers.forEach {
            it.write(LogRecord.PlainText(
                name = name,
                msg = msg,
                level = level,
                attr = attrs.plus(attr)
            ))
        }
    }

    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> print(msg: T) {
        providers.forEach {
            it.write(LogRecord.StructuredText(
                name = name,
                msg = msg,
                level = level,
                serializer = T::class.serializer(),
                attr = attrs
            ))
        }
    }

    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> print(
        msg: T,
        attr: Map<String, String>
    ) {
        providers.forEach {
            it.write(LogRecord.StructuredText(
                name = name,
                msg = msg,
                level = level,
                serializer = T::class.serializer(),
                attr = attrs.plus(attr)
            ))
        }
    }
}