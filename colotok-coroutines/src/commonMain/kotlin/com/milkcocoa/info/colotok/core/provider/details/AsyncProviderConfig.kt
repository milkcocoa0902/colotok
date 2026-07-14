package com.milkcocoa.info.colotok.core.provider.details

interface AsyncProviderConfig : ProviderConfig {
    /**
     * Number of log entries to buffer before sending.
     *
     * Valid values are `1..4096`. After a publish failure, records may be
     * retained up to four times this threshold, capped at 4096 records.
     */
    var bufferSize: Int
}
