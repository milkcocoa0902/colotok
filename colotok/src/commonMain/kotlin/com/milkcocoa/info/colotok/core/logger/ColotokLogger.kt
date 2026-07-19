package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

class ColotokLogger(
    val name: String,
    private val providersProvider: () -> List<Provider>,
    private val attrsProvider: () -> Map<String, String>
) {
    val providers get() = providersProvider()
    val attrs get() = attrsProvider()

    constructor(name: String, config: ColotokConfig) : this(
        name = name,
        providersProvider = { config.providers },
        attrsProvider = { config.defaultAttrs }
    )

    constructor(name: String, config: ColotokConfig.() -> Unit) : this(name = name, ColotokConfig().apply(config))

    /**
     * print log with providers into passed [level]
     * @param msg[String] message to print
     * @param level[Level] log level
     */
    fun at(
        level: Level,
        msg: String
    ) {
        val p = providers
        if (p.none { level.isEnabledFor(it.config.level) }) return
        val record =
            LogRecord.PlainText(
                name = name,
                msg = msg,
                level = level,
                attr = attrs.toMap()
            )
        p.forEach {
            it.write(record)
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
        val p = providers
        if (p.none { level.isEnabledFor(it.config.level) }) return
        val record =
            LogRecord.PlainText(
                name = name,
                msg = msg,
                level = level,
                attr = attrs.plus(attr)
            )
        p.forEach {
            it.write(record)
        }
    }

    /**
     * print structured log with providers into passed [level]
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    inline fun <reified T : LogStructure> at(
        level: Level,
        msg: T
    ) {
        val p = providers
        if (p.none { level.isEnabledFor(it.config.level) }) return
        val record =
            LogRecord.StructuredText(
                name = name,
                msg = msg,
                level = level,
                serializer = serializer<T>(),
                attr = attrs.toMap()
            )
        p.forEach {
            it.write(record)
        }
    }

    /**
     * print structured log and attrs with providers into passed [level]
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     * @param attr[Map] additional attributes
     */
    inline fun <reified T : LogStructure> at(
        level: Level,
        msg: T,
        attr: Map<String, String>
    ) {
        val p = providers
        if (p.none { level.isEnabledFor(it.config.level) }) return
        val record =
            LogRecord.StructuredText(
                name = name,
                msg = msg,
                level = level,
                serializer = serializer<T>(),
                attr = attrs.plus(attr)
            )
        p.forEach {
            it.write(record)
        }
    }

    /**
     * Shutdown the logger and wait for all providers to finish processing.
     */
    suspend fun shutdown() {
        var firstFailure: Throwable? = null
        providers.forEach { provider ->
            try {
                provider.join()
            } catch (throwable: Throwable) {
                if (firstFailure == null) firstFailure = throwable
            }
        }
        firstFailure?.let { throw it }
    }

    /**
     * Shutdown the logger immediately.
     */
    fun forceShutdown() {
        var firstFailure: Throwable? = null
        providers.forEach { provider ->
            try {
                provider.forceShutdown()
            } catch (throwable: Throwable) {
                if (firstFailure == null) firstFailure = throwable
            }
        }
        firstFailure?.let { throw it }
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
    inline fun <reified T : LogStructure> trace(msg: T) {
        at(LogLevel.TRACE, msg)
    }

    /**
     * print debug level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    inline fun <reified T : LogStructure> debug(msg: T) {
        at(LogLevel.DEBUG, msg)
    }

    /**
     * print info level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    inline fun <reified T : LogStructure> info(msg: T) {
        at(LogLevel.INFO, msg)
    }

    /**
     * print warn level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    inline fun <reified T : LogStructure> warn(msg: T) {
        at(LogLevel.WARN, msg)
    }

    /**
     * print error level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     */
    inline fun <reified T : LogStructure> error(msg: T) {
        at(LogLevel.ERROR, msg)
    }

    /**
     * print trace level log with providers
     * @param msg[LogStructure] message to print
     * @param serializer[KSerializer] serializer which used for serialize [msg]
     * @param attr[Map] additional attributes
     */
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
        val p = providers
        if (p.none { level.isEnabledFor(it.config.level) }) return
        LevelScopedColotokLogger(
            name = name,
            providers = p,
            attrs = attrs,
            level = level
        ).block()
    }
}

val Colotok get() = ColotokLoggerContext.DEFAULT.getLogger()