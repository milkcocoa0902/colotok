package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import kotlinx.serialization.KSerializer

final class LevelScopedLogger(val name: String, val config: Config, private val level: Level) {
    private val providers = config.providers
    private val attrs = config.defaultAttrs

    fun print(msg: String){
        if(this.attrs.isEmpty()){
            providers.forEach {
                it.write(name, msg, level)
            }
        }else{
            providers.forEach {
                it.write(name, msg, level, this.attrs)
            }
        }
    }

    fun print(msg: String, attr: Map<String, String>){
        providers.forEach {
            it.write(name, msg, level, attr.plus(this.attrs))
        }
    }

    fun<T: LogStructure> print(msg: T, serializer: KSerializer<T>){
        if(this.attrs.isEmpty()){
            providers.forEach {
                it.write(name, msg, serializer, level)
            }
        }else{
            providers.forEach {
                it.write(name, msg, serializer, level, this.attrs)
            }
        }
    }
    fun<T: LogStructure> print(msg: T, serializer: KSerializer<T>, attr: Map<String, String>){
        providers.forEach {
            it.write(name, msg, serializer, level, attr.plus(this.attrs))
        }
    }
}