package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.LogLevel
import kotlinx.serialization.KSerializer

class Logger(val name: String, val config: Config) {
    constructor(name: String, config: Config.() -> Unit) : this(name = name, Config().apply(config))

    private val providers = config.providers

    /**
     * print trace level log with providers
     * @param msg[String] message to print
     */
    fun trace(msg: String) {
        providers.forEach {
            it.write(name, msg, LogLevel.TRACE)
        }
    }

    /**
     * print debug level log with providers
     * @param msg[String] message to print
     */
    fun debug(msg: String) {
        providers.forEach {
            it.write(name, msg, LogLevel.DEBUG)
        }
    }

    /**
     * print info level log with providers
     * @param msg[String] message to print
     */
    fun info(msg: String) {
        providers.forEach {
            it.write(name, msg, LogLevel.INFO)
        }
    }

    /**
     * print warn level log with providers
     * @param msg[String] message to print
     */
    fun warn(msg: String) {
        providers.forEach {
            it.write(name, msg, LogLevel.WARN)
        }
    }

    /**
     * print error level log with providers
     * @param msg[String] message to print
     */
    fun error(msg: String) {
        providers.forEach {
            it.write(name, msg, LogLevel.ERROR)
        }
    }




    /**
     * print trace level log with providers
     * @param msg[String] message to print
     */
    fun trace(msg: String, attr: Map<String, String>) {
        providers.forEach {
            it.write(name, msg, LogLevel.TRACE, attr)
        }
    }

    /**
     * print debug level log with providers
     * @param msg[String] message to print
     */
    fun debug(msg: String, attr: Map<String, String>) {
        providers.forEach {
            it.write(name, msg, LogLevel.DEBUG, attr)
        }
    }

    /**
     * print info level log with providers
     * @param msg[String] message to print
     */
    fun info(msg: String, attr: Map<String, String>) {
        providers.forEach {
            it.write(name, msg, LogLevel.INFO, attr)
        }
    }

    /**
     * print warn level log with providers
     * @param msg[String] message to print
     */
    fun warn(msg: String, attr: Map<String, String>) {
        providers.forEach {
            it.write(name, msg, LogLevel.WARN, attr)
        }
    }

    /**
     * print error level log with providers
     * @param msg[String] message to print
     */
    fun error(msg: String, attr: Map<String, String>) {
        providers.forEach {
            it.write(name, msg, LogLevel.ERROR, attr)
        }
    }





    /**
     * print trace level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    fun<T: LogStructure> trace(msg: T, serializer: KSerializer<T>) {
        providers.forEach {
            it.write(name, msg, serializer, LogLevel.TRACE)
        }
    }


    /**
     * print debug level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    fun<T: LogStructure> debug(msg: T, serializer: KSerializer<T>) {
        providers.forEach {
            it.write(name, msg, serializer, LogLevel.DEBUG)
        }
    }


    /**
     * print info level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    fun<T: LogStructure> info(msg: T, serializer: KSerializer<T>) {
        providers.forEach {
            it.write(name, msg, serializer, LogLevel.INFO)
        }
    }


    /**
     * print warn level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    fun<T: LogStructure> warn(msg: T, serializer: KSerializer<T>) {
        providers.forEach {
            it.write(name, msg, serializer, LogLevel.WARN)
        }
    }


    /**
     * print error level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    fun<T: LogStructure> error(msg: T, serializer: KSerializer<T>) {
        providers.forEach {
            it.write(name, msg, serializer, LogLevel.ERROR)
        }
    }




    /**
     * print trace level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    fun<T: LogStructure> trace(msg: T, serializer: KSerializer<T>, attr: Map<String, String>) {
        providers.forEach {
            it.write(name, msg, serializer, LogLevel.TRACE, attr)
        }
    }


    /**
     * print debug level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    fun<T: LogStructure> debug(msg: T, serializer: KSerializer<T>, attr: Map<String, String>) {
        providers.forEach {
            it.write(name, msg, serializer, LogLevel.DEBUG, attr)
        }
    }


    /**
     * print info level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    fun<T: LogStructure> info(msg: T, serializer: KSerializer<T>, attr: Map<String, String>) {
        providers.forEach {
            it.write(name, msg, serializer, LogLevel.INFO, attr)
        }
    }


    /**
     * print warn level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    fun<T: LogStructure> warn(msg: T, serializer: KSerializer<T>, attr: Map<String, String>) {
        providers.forEach {
            it.write(name, msg, serializer, LogLevel.WARN, attr)
        }
    }


    /**
     * print error level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    fun<T: LogStructure> error(msg: T, serializer: KSerializer<T>, attr: Map<String, String>) {
        providers.forEach {
            it.write(name, msg, serializer, LogLevel.ERROR, attr)
        }
    }





    /**
     * create trace level scope.
     * you can use [LevelScopedLogger.print] in this block.
     * which method print with providers in trace level
     * @param block action in scope
     * @see LevelScopedLogger
     */
    fun atTrace(block: LevelScopedLogger.() -> Unit){
        block(LevelScopedLogger(name, config, LogLevel.TRACE))
    }


    /**
     * create debug level scope.
     * you can use [LevelScopedLogger.print] in this block.
     * which method print with providers in trace level
     * @param block action in scope
     * @see LevelScopedLogger
     */
    fun atDebug(block: LevelScopedLogger.() -> Unit){
        block(LevelScopedLogger(name, config, LogLevel.DEBUG))
    }


    /**
     * create info level scope.
     * you can use [LevelScopedLogger.print] in this block.
     * which method print with providers in trace level
     * @param block action in scope
     * @see LevelScopedLogger
     */
    fun atInfo(block: LevelScopedLogger.() -> Unit){
        block(LevelScopedLogger(name, config, LogLevel.INFO))
    }


    /**
     * create warn level scope.
     * you can use [LevelScopedLogger.print] in this block.
     * which method print with providers in trace level
     * @param block action in scope
     * @see LevelScopedLogger
     */
    fun atWarn(block: LevelScopedLogger.() -> Unit){
        block(LevelScopedLogger(name, config, LogLevel.WARN))
    }


    /**
     * create error level scope.
     * you can use [LevelScopedLogger.print] in this block.
     * which method print with providers in trace level
     * @param block action in scope
     * @see LevelScopedLogger
     */
    fun atError(block: LevelScopedLogger.() -> Unit){
        block(LevelScopedLogger(name, config, LogLevel.ERROR))
    }
}