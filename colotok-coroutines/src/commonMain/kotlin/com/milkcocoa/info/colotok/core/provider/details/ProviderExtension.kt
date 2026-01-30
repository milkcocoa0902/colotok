package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer

suspend fun IProvider.writeAsync(
    record: LogRecord
) {
    if(this is AsyncProvider) {
        this.writeAsync(record)
        return
    }
    withContext(Dispatchers.Default){
        write(record)
    }
}