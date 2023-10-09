package com.milkcocoa.info.clk.core

import com.milkcocoa.info.clk.core.provider.details.Provider

class LoggerFactory {
    private val providers: MutableList<Provider> = mutableListOf()
    private var logLevel: LogLevel = LogLevel.DEBUG
    fun addProvider(provider: Provider) = apply {
        providers.add(provider)
    }

    fun setLogLevel(level: LogLevel) = apply { logLevel = level }

    fun getLogger(name: String = "Default Logger"): Logger {
        return Logger(name = name) {
            this.providers = this@LoggerFactory.providers
            this.level = this@LoggerFactory.logLevel
        }
    }
}