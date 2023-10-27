package com.milkcocoa.info.clk.core.logger

import com.milkcocoa.info.clk.core.provider.details.Provider

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