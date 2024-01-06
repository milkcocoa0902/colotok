package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.provider.details.Provider

/**
 * builder class for get logger instance
 */
class LoggerFactory {
    private val providers: MutableList<Provider> = mutableListOf()
    private val attrs = mutableMapOf<String, String>()

    fun withAttrs(attr: Map<String, String>) = apply {
        this@LoggerFactory.attrs.putAll(attr)
    }

    /**
     * add provider to current logger
     * @param provider[Provider] provider to print log
     * @return [LoggerFactory] this instance
     */
    fun addProvider(provider: Provider) = apply {
        providers.add(provider)
    }

    /**
     * generate logger instance with default name
     * @return [Logger] logger
     */
    fun getLogger(): Logger{
        return Logger(name = "Default Logger") {
            this.defaultAttrs = this@LoggerFactory.attrs
            this.providers = this@LoggerFactory.providers
        }
    }

    /**
     * generate logger instance with specified name
     * @param name[String] logger name
     * @return [Logger] logger
     */
    fun getLogger(name: String): Logger {
        return Logger(name = name) {
            this.defaultAttrs = this@LoggerFactory.attrs
            this.providers = this@LoggerFactory.providers
        }
    }
}