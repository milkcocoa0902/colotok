package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.provider.details.Provider

/**
 * builder class for get logger instance
 */
class ColotokLoggerFactory {
    private val providers: MutableList<Provider> = mutableListOf()
    private val attrs = mutableMapOf<String, String>()

    fun withAttrs(attr: Map<String, String>) =
        apply {
            this@ColotokLoggerFactory.attrs.putAll(attr)
        }

    /**
     * add provider to current logger
     * @param provider[Provider] provider to print log
     * @return [ColotokLoggerFactory] this instance
     */
    fun addProvider(provider: Provider) =
        apply {
            providers.add(provider)
        }

    /**
     * generate logger instance with default name
     * @return [ColotokLogger] logger
     */
    fun getLogger(): ColotokLogger {
        return ColotokLogger(name = "Default Logger") {
            this.defaultAttrs = this@ColotokLoggerFactory.attrs
            this.providers = this@ColotokLoggerFactory.providers
        }
    }

    /**
     * generate logger instance with specified name
     * @param name[String] logger name
     * @return [ColotokLogger] logger
     */
    fun getLogger(name: String): ColotokLogger {
        return ColotokLogger(name = name) {
            this.defaultAttrs = this@ColotokLoggerFactory.attrs
            this.providers = this@ColotokLoggerFactory.providers
        }
    }
}