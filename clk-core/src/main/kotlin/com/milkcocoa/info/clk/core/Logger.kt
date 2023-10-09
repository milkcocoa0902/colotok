package com.milkcocoa.info.clk.core

import com.milkcocoa.info.clk.core.provider.details.Provider

class Logger(val name: String, val config: Logger.Config) {
    class Config {
        var providers = listOf<Provider>()
    }
    constructor(name: String, config: Config.() -> Unit) : this(name = name, Config().apply(config))

    private val providers = config.providers
    fun trace(str: String) {
        providers.forEach {
            it.write(name, str, LogLevel.TRACE)
        }
    }

    fun debug(str: String) {
        providers.forEach {
            it.write(name, str, LogLevel.DEBUG)
        }
    }

    fun info(str: String) {
        providers.forEach {
            it.write(name, str, LogLevel.INFO)
        }
    }

    fun warn(str: String) {
        providers.forEach {
            it.write(name, str, LogLevel.WARN)
        }
    }

    fun error(str: String) {
        providers.forEach {
            it.write(name, str, LogLevel.ERROR)
        }
    }
}