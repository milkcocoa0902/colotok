package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.logger.LogRecord

interface Provider {
    fun write(record: LogRecord)
}