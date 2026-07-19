package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun IProvider.writeAsync(record: LogRecord) {
    if (this is AsyncProvider) {
        this.writeAsync(record)
        return
    }
    withContext(Dispatchers.Default) {
        write(record)
    }
}