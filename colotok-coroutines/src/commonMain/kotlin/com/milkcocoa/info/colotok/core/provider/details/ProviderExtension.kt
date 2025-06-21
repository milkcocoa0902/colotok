package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer

suspend fun Provider.writeAsync(
    name: String,
    msg: String,
    level: Level
) {
    if(this is AsyncProvider) {
        this.writeAsync(name, msg, level)
        return
    }
    withContext(Dispatchers.Default){
        write(name, msg, level, mapOf())
    }
}


suspend fun Provider.writeAsync(
    name: String,
    msg: String,
    level: Level,
    attr: Map<String, String>
){
    if(this is AsyncProvider) {
        this.writeAsync(name, msg, level, attr)
        return
    }
    withContext(Dispatchers.Default){
        write(name, msg, level, attr)
    }
}


suspend fun <T : LogStructure> Provider.writeAsync(
    name: String,
    msg: T,
    serializer: KSerializer<T>,
    level: Level
) {
    if(this is AsyncProvider) {
        this.writeAsync(name, msg, serializer, level)
        return
    }
    withContext(Dispatchers.Default){
        write(name, msg, serializer, level)
    }
}


suspend fun <T : LogStructure> Provider.writeAsync(
    name: String,
    msg: T,
    serializer: KSerializer<T>,
    level: Level,
    attr: Map<String, String>
){
    if(this is AsyncProvider) {
        this.writeAsync(name, msg, serializer, level)
        return
    }
    withContext(Dispatchers.Default){
        write(name, msg, serializer, level, attr)
    }
}
