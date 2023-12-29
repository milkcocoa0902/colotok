package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import kotlinx.serialization.KSerializer

final class LevelScopedLogger(val name: String, val config: Config, private val level: LogLevel) {
    private val providers = config.providers

    fun print(msg: String){
        print(msg, mapOf())
    }

    fun print(msg: String, attr: Map<String, String>){
        providers.forEach {
            it.write(name, msg, level, attr)
        }
    }

    fun<T: LogStructure> print(msg: T, serializer: KSerializer<T>){
        print(msg, serializer, mapOf())
    }
    fun<T: LogStructure> print(msg: T, serializer: KSerializer<T>, attr: Map<String, String>){
        providers.forEach {
            it.write(name, msg, serializer, level, attr)
        }
    }
}