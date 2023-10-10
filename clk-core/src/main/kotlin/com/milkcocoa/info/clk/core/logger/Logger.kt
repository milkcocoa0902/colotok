package com.milkcocoa.info.clk.core.logger

class Logger(val name: String, val config: Config) {
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

    fun atTrace(block: LevelScopedLogger.() -> Unit){
        block(LevelScopedLogger(name, config, LogLevel.TRACE))
    }

    fun atDebug(block: LevelScopedLogger.() -> Unit){
        block(LevelScopedLogger(name, config, LogLevel.DEBUG))
    }

    fun atInfo(block: LevelScopedLogger.() -> Unit){
        block(LevelScopedLogger(name, config, LogLevel.INFO))
    }

    fun atWarn(block: LevelScopedLogger.() -> Unit){
        block(LevelScopedLogger(name, config, LogLevel.WARN))
    }

    fun atError(block: LevelScopedLogger.() -> Unit){
        block(LevelScopedLogger(name, config, LogLevel.ERROR))
    }
}