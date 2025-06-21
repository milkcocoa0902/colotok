package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProvider
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

class ColotokLogger(val name: String, config: ColotokConfig) {
    constructor(name: String, config: ColotokConfig.() -> Unit) : this(name = name, ColotokConfig().apply(config))

    val providers = config.providers
    val attrs = config.defaultAttrs

    /**
     * print log with providers into passed [level]
     * @param msg[String] message to print
     * @param level[Level] log level
     */
    fun at(
        level: Level,
        msg: String
    ) {
        if (attrs.isEmpty()) {
            providers.forEach {
                it.write(name, msg, level)
            }
        } else {
            providers.forEach {
                it.write(name, msg, level, attrs)
            }
        }
    }

    /**
     * print log and attrs with providers into passed [level]
     * @param level[Level] log level
     * @param msg[String] message to print
     * @param attr[Map] additional attrs
     */
    fun at(
        level: Level,
        msg: String,
        attr: Map<String, String>
    ) {
        providers.forEach {
            it.write(name, msg, level, attr.plus(this.attrs))
        }
    }

    /**
     * print structured log with providers into passed [level]
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> at(
        level: Level,
        msg: T
    ) {
        if (this.attrs.isEmpty()) {
            providers.forEach {
                it.write(name, msg, T::class.serializer(), level)
            }
        } else {
            providers.forEach {
                it.write(name, msg, T::class.serializer(), level, this.attrs)
            }
        }
    }

    /**
     * print structured log and attrs with providers into passed [level]
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     * @param attr[Map] additional attributes
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> at(
        level: Level,
        msg: T,
        attr: Map<String, String>
    ) {
        providers.forEach {
            it.write(name, msg, T::class.serializer(), level, attr.plus(this.attrs))
        }
    }

    /**
     * print trace level log with providers
     * @param msg[String] message to print
     */
    fun trace(msg: String) {
        at(LogLevel.TRACE, msg)
    }

    /**
     * print debug level log with providers
     * @param msg[String] message to print
     */
    fun debug(msg: String) {
        at(LogLevel.DEBUG, msg)
    }

    /**
     * print info level log with providers
     * @param msg[String] message to print
     */
    fun info(msg: String) {
        at(LogLevel.INFO, msg)
    }

    /**
     * print warn level log with providers
     * @param msg[String] message to print
     */
    fun warn(msg: String) {
        at(LogLevel.WARN, msg)
    }

    /**
     * print error level log with providers
     * @param msg[String] message to print
     */
    fun error(msg: String) {
        at(LogLevel.ERROR, msg)
    }

    /**
     * print trace level log with providers
     * @param msg[String] message to print
     * @param attr[Map] additional attrs
     */
    fun trace(
        msg: String,
        attr: Map<String, String>
    ) {
        at(LogLevel.TRACE, msg, attr)
    }

    /**
     * print debug level log with providers
     * @param msg[String] message to print
     * @param attr[Map] additional attrs
     */
    fun debug(
        msg: String,
        attr: Map<String, String>
    ) {
        at(LogLevel.DEBUG, msg, attr)
    }

    /**
     * print info level log with providers
     * @param msg[String] message to print
     * @param attr[Map] additional attrs
     */
    fun info(
        msg: String,
        attr: Map<String, String>
    ) {
        at(LogLevel.INFO, msg, attr)
    }

    /**
     * print warn level log with providers
     * @param msg[String] message to print
     * @param attr[Map] additional attrs
     */
    fun warn(
        msg: String,
        attr: Map<String, String>
    ) {
        at(LogLevel.WARN, msg, attr)
    }

    /**
     * print error level log with providers
     * @param msg[String] message to print
     * @param attr[Map] additional attrs
     */
    fun error(
        msg: String,
        attr: Map<String, String>
    ) {
        at(LogLevel.ERROR, msg, attr)
    }

    /**
     * print trace level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> trace(msg: T) {
        at(LogLevel.TRACE, msg)
    }

    /**
     * print debug level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> debug(msg: T) {
        at(LogLevel.DEBUG, msg)
    }

    /**
     * print info level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> info(msg: T) {
        at(LogLevel.INFO, msg)
    }

    /**
     * print warn level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> warn(msg: T) {
        at(LogLevel.WARN, msg)
    }

    /**
     * print error level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> error(msg: T) {
        at(LogLevel.ERROR, msg)
    }

    /**
     * print trace level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     * @param attr[Map] additional attributes
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> trace(
        msg: T,
        attr: Map<String, String>
    ) {
        at(LogLevel.TRACE, msg, attr)
    }

    /**
     * print debug level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     * @param attr[Map] additional attrs
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> debug(
        msg: T,
        attr: Map<String, String>
    ) {
        at(LogLevel.DEBUG, msg, attr)
    }

    /**
     * print info level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     * @param attr[Map] additional attrs
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> info(
        msg: T,
        attr: Map<String, String>
    ) {
        at(LogLevel.INFO, msg, attr)
    }

    /**
     * print warn level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     * @param attr[Map] additional attrs
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> warn(
        msg: T,
        attr: Map<String, String>
    ) {
        at(LogLevel.WARN, msg, attr)
    }

    /**
     * print error level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     * @param attr[Map] additional attrs
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : LogStructure> error(
        msg: T,
        attr: Map<String, String>
    ) {
        at(LogLevel.ERROR, msg, attr)
    }

    /**
     * create trace level scope.
     * you can use [LevelScopedColotokLogger.print] in this block.
     * which method print with providers in trace level
     * @param block action in scope
     * @see LevelScopedColotokLogger
     */
    inline fun atTrace(block: LevelScopedColotokLogger.() -> Unit) {
        at(LogLevel.TRACE, block)
    }

    /**
     * create debug level scope.
     * you can use [LevelScopedColotokLogger.print] in this block.
     * which method print with providers in trace level
     * @param block action in scope
     * @see LevelScopedColotokLogger
     */
    inline fun atDebug(block: LevelScopedColotokLogger.() -> Unit) {
        at(LogLevel.DEBUG, block)
    }

    /**
     * create info level scope.
     * you can use [LevelScopedColotokLogger.print] in this block.
     * which method print with providers in trace level
     * @param block action in scope
     * @see LevelScopedColotokLogger
     */
    inline fun atInfo(block: LevelScopedColotokLogger.() -> Unit) {
        at(LogLevel.INFO, block)
    }

    /**
     * create warn level scope.
     * you can use [LevelScopedColotokLogger.print] in this block.
     * which method print with providers in trace level
     * @param block action in scope
     * @see LevelScopedColotokLogger
     */
    inline fun atWarn(block: LevelScopedColotokLogger.() -> Unit) {
        at(LogLevel.WARN, block)
    }

    /**
     * create error level scope.
     * you can use [LevelScopedColotokLogger.print] in this block.
     * which method print with providers in trace level
     * @param block action in scope
     * @see LevelScopedColotokLogger
     */
    inline fun atError(block: LevelScopedColotokLogger.() -> Unit) {
        at(LogLevel.ERROR, block)
    }

    /**
     * create specified level scope.
     * you can use [LevelScopedColotokLogger.print] in this block.
     * which method print with providers in trace level
     * @param block action in scope
     * @see LevelScopedColotokLogger
     */
    inline fun at(
        level: Level,
        block: LevelScopedColotokLogger.() -> Unit
    ) {
        LevelScopedColotokLogger(
            name = name,
            config =
                ColotokConfig().apply {
                    this.providers = this@ColotokLogger.providers
                    this.defaultAttrs = this@ColotokLogger.attrs
                },
            level = level
        ).block()
    }

    companion object {
        private var defaultLogger: ColotokLogger =
            ColotokLogger(
                name = "default logger"
            ) {
                providers =
                    listOf(
                        ConsoleProvider(
                            ConsoleProviderConfig().apply {
                                level = LogLevel.DEBUG
                                formatter = DetailTextFormatter
                            }
                        )
                    )
            }

        fun getDefault(): ColotokLogger {
            return defaultLogger
        }

        fun setDefault(logger: ColotokLogger) {
            defaultLogger = logger
        }
    }
}