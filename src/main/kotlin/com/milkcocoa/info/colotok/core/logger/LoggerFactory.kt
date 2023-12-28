package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.provider.details.Provider

class LoggerFactory {
    private val providers: MutableList<Provider> = mutableListOf()
    fun addProvider(provider: Provider) = apply {
        providers.add(provider)
    }


    fun getLogger(name: String = "Default Logger"): Logger {
        return Logger(name = name) {
            this.providers = this@LoggerFactory.providers
        }
    }
}