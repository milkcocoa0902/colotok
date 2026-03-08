package com.milkcocoa.info.colotok.core.provider.details

interface AsyncProviderConfig : ProviderConfig {
    /**
     * Number of log entries to buffer before sending
     */
    var bufferSize: Int
}