package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level

/**
 * provider's basic config
 */
interface ProviderConfig {
    /**
     * providers log level
     */
    var level: Level

    /**
     * formatter
     */
    var formatter: Formatter
}