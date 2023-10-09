package com.milkcocoa.info.clk.core

import com.milkcocoa.info.clk.core.provider.details.Provider
import com.milkcocoa.info.clk.util.color.ColorExtension.blue
import com.milkcocoa.info.clk.util.color.ColorExtension.green
import com.milkcocoa.info.clk.util.color.ColorExtension.red
import com.milkcocoa.info.clk.util.color.ColorExtension.white
import com.milkcocoa.info.clk.util.color.ColorExtension.yellow

class Logger(val name: String, val config: Logger.Config) {
    class Config{
        var providers = listOf<Provider>()
        var level = LogLevel.DEBUG
    }
    constructor(name: String, config: Config.() -> Unit): this(name = name, Config().apply(config))

    private val providers = config.providers
    private val level = config.level
    fun trace(str: String){
        if(level.toInt() > LogLevel.TRACE.toInt()) return
        providers.forEach{
            it.write(name, str, LogLevel.TRACE)
        }
    }

    fun debug(str: String){
        if(level.toInt() > LogLevel.DEBUG.toInt()) return
        providers.forEach{
            it.write(name, str, LogLevel.DEBUG)
        }
    }

    fun info(str: String){
        if(level.toInt() > LogLevel.INFO.toInt()) return
        providers.forEach{
            it.write(name, str, LogLevel.INFO)
        }
    }

    fun warn(str: String){
        if(level.toInt() > LogLevel.WARN.toInt()) return
        providers.forEach{
            it.write(name, str, LogLevel.WARN)
        }
    }

    fun error(str: String){
        if(level.toInt() > LogLevel.ERROR.toInt()) return
        providers.forEach{
            it.write(name, str, LogLevel.ERROR)
        }
    }
}