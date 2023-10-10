package com.milkcocoa.info.clk.core.logger

final class LevelScopedLogger(val name: String, val config: Config, private val level: LogLevel) {
    private val providers = config.providers

    fun print(msg: String){
        providers.forEach {
            it.write(name, msg, level)
        }
    }
}