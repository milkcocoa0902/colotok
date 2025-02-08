package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

final class LevelScopedLogger(
    val name: String,
    private val config: Config,
    val level: Level
) {
    val providers get() = config.providers
    val attrs get() = config.defaultAttrs

    fun print(msg: String) {
        if (this.attrs.isEmpty()) {
            providers.forEach {
                it.write(name, msg, level)
            }
        } else {
            providers.forEach {
                it.write(name, msg, level, this.attrs)
            }
        }
    }

    fun print(
        msg: String,
        attr: Map<String, String>
    ) {
        providers.forEach {
            it.write(name, msg, level, attr.plus(this.attrs))
        }
    }

    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> print(
        msg: T,
    ) {
        if (this.attrs.isEmpty()) {
            providers.forEach {
                it.write(name, msg, T::class.serializer(), level)
            }
        } else {
            providers.forEach {
                it.write(name, msg, T::class.serializer(), level, this.attrs)
            }
        }
    }

    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> print(
        msg: T,
        attr: Map<String, String>
    ) {
        providers.forEach {
            it.write(name, msg, T::class.serializer(), level, attr.plus(this.attrs))
        }
    }
}