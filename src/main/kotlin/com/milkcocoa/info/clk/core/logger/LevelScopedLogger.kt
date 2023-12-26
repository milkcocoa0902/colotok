package com.milkcocoa.info.clk.core.logger

import com.milkcocoa.info.clk.core.formatter.details.LogStructure
import kotlinx.serialization.KSerializer

final class LevelScopedLogger(val name: String, val config: Config, private val level: LogLevel) {
    private val providers = config.providers

    fun print(msg: String){
        providers.forEach {
            it.write(name, msg, level)
        }
    }

    fun<T: LogStructure> print(msg: T, serializer: KSerializer<T>){
        providers.forEach {
            it.write(name, msg, serializer, level)
        }
    }
}