package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.provider.details.Provider

/**
 * logger configuration object
 */
class ColotokConfig {
    /**
     * providers to print log
     */
    var providers = listOf<Provider>()

    var defaultAttrs = mapOf<String, String>()
}