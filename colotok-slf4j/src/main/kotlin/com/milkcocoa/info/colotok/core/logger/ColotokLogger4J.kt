package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.helpers.MessageFormatter

class ColotokLogger4J(private val name: String) : Logger {
    private data class FormattedMessage(
        val message: String,
        val throwable: Throwable?
    )

    private val delegate: ColotokLogger by lazy {
        ColotokLoggerContext.DEFAULT
            .shallowCopy()
            .putAttrs(mapOf("logger" to name))
            .getLogger(name)
    }

    private fun formatMessage(
        pattern: String?,
        vararg arguments: Any?
    ): FormattedMessage {
        val tuple = MessageFormatter.arrayFormat(pattern.orEmpty(), arguments)
        return FormattedMessage(
            message = tuple.message.orEmpty(),
            throwable = tuple.throwable
        )
    }

    private fun log(
        level: Level,
        message: String,
        throwable: Throwable? = null
    ) {
        if (!isEnabled(level)) return
        logEnabled(level, message, throwable)
    }

    private fun logEnabled(
        level: Level,
        message: String,
        throwable: Throwable?
    ) {
        if (throwable == null) {
            delegate.at(level, message)
        } else {
            delegate.at(level, message, mapOf("cause" to throwable.stackTraceToString()))
        }
    }

    private fun isEnabled(level: Level): Boolean = delegate.providers.any { level.isEnabledFor(it.config.level) }

    private fun logFormatted(
        level: Level,
        pattern: String?,
        vararg arguments: Any?
    ) {
        if (!isEnabled(level)) return
        val formatted = formatMessage(pattern, *arguments)
        logEnabled(level, formatted.message, formatted.throwable)
    }

    override fun info(msg: String?) {
        log(LogLevel.INFO, msg.orEmpty())
    }

    override fun info(
        format: String?,
        arg: Any?
    ) {
        logFormatted(LogLevel.INFO, format, arg)
    }

    override fun info(
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        logFormatted(LogLevel.INFO, format, arg1, arg2)
    }

    override fun info(
        format: String?,
        vararg arguments: Any?
    ) {
        logFormatted(LogLevel.INFO, format, *arguments)
    }

    override fun info(
        msg: String?,
        t: Throwable?
    ) {
        log(LogLevel.INFO, msg.orEmpty(), t)
    }

    override fun isInfoEnabled(marker: Marker?): Boolean = isInfoEnabled

    override fun info(
        marker: Marker?,
        msg: String?
    ) {
        info(msg)
    }

    override fun info(
        marker: Marker?,
        format: String?,
        arg: Any?
    ) {
        info(format, arg)
    }

    override fun info(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        info(format, arg1, arg2)
    }

    override fun info(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?
    ) {
        info(format, *arguments)
    }

    override fun info(
        marker: Marker?,
        msg: String?,
        t: Throwable?
    ) {
        info(msg, t)
    }

    override fun isWarnEnabled(): Boolean = isEnabled(LogLevel.WARN)

    override fun debug(msg: String?) {
        log(LogLevel.DEBUG, msg.orEmpty())
    }

    override fun debug(
        format: String?,
        arg: Any?
    ) {
        logFormatted(LogLevel.DEBUG, format, arg)
    }

    override fun debug(
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        logFormatted(LogLevel.DEBUG, format, arg1, arg2)
    }

    override fun debug(
        format: String?,
        vararg arguments: Any?
    ) {
        logFormatted(LogLevel.DEBUG, format, *arguments)
    }

    override fun debug(
        msg: String?,
        t: Throwable?
    ) {
        log(LogLevel.DEBUG, msg.orEmpty(), t)
    }

    override fun isDebugEnabled(marker: Marker?): Boolean = isDebugEnabled

    override fun debug(
        marker: Marker?,
        msg: String?
    ) {
        debug(msg)
    }

    override fun debug(
        marker: Marker?,
        format: String?,
        arg: Any?
    ) {
        debug(format, arg)
    }

    override fun debug(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        debug(format, arg1, arg2)
    }

    override fun debug(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?
    ) {
        debug(format, *arguments)
    }

    override fun debug(
        marker: Marker?,
        msg: String?,
        t: Throwable?
    ) {
        debug(msg, t)
    }

    override fun isInfoEnabled(): Boolean = isEnabled(LogLevel.INFO)

    override fun warn(msg: String?) {
        log(LogLevel.WARN, msg.orEmpty())
    }

    override fun warn(
        format: String?,
        arg: Any?
    ) {
        logFormatted(LogLevel.WARN, format, arg)
    }

    override fun warn(
        format: String?,
        vararg arguments: Any?
    ) {
        logFormatted(LogLevel.WARN, format, *arguments)
    }

    override fun warn(
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        logFormatted(LogLevel.WARN, format, arg1, arg2)
    }

    override fun warn(
        msg: String?,
        t: Throwable?
    ) {
        log(LogLevel.WARN, msg.orEmpty(), t)
    }

    override fun isWarnEnabled(marker: Marker?): Boolean = isWarnEnabled

    override fun warn(
        marker: Marker?,
        msg: String?
    ) {
        warn(msg)
    }

    override fun warn(
        marker: Marker?,
        format: String?,
        arg: Any?
    ) {
        warn(format, arg)
    }

    override fun warn(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        warn(format, arg1, arg2)
    }

    override fun warn(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?
    ) {
        warn(format, *arguments)
    }

    override fun warn(
        marker: Marker?,
        msg: String?,
        t: Throwable?
    ) {
        warn(msg, t)
    }

    override fun isErrorEnabled(): Boolean = isEnabled(LogLevel.ERROR)

    override fun error(msg: String?) {
        log(LogLevel.ERROR, msg.orEmpty())
    }

    override fun error(
        format: String?,
        arg: Any?
    ) {
        logFormatted(LogLevel.ERROR, format, arg)
    }

    override fun error(
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        logFormatted(LogLevel.ERROR, format, arg1, arg2)
    }

    override fun error(
        format: String?,
        vararg arguments: Any?
    ) {
        logFormatted(LogLevel.ERROR, format, *arguments)
    }

    override fun error(
        msg: String?,
        t: Throwable?
    ) {
        log(LogLevel.ERROR, msg.orEmpty(), t)
    }

    override fun isErrorEnabled(marker: Marker?): Boolean = isErrorEnabled

    override fun error(
        marker: Marker?,
        msg: String?
    ) {
        error(msg)
    }

    override fun error(
        marker: Marker?,
        format: String?,
        arg: Any?
    ) {
        error(format, arg)
    }

    override fun error(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        error(format, arg1, arg2)
    }

    override fun error(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?
    ) {
        error(format, *arguments)
    }

    override fun error(
        marker: Marker?,
        msg: String?,
        t: Throwable?
    ) {
        error(msg, t)
    }

    override fun getName(): String = name

    override fun isTraceEnabled(): Boolean = isEnabled(LogLevel.TRACE)

    override fun trace(msg: String?) {
        log(LogLevel.TRACE, msg.orEmpty())
    }

    override fun trace(
        format: String?,
        arg: Any?
    ) {
        logFormatted(LogLevel.TRACE, format, arg)
    }

    override fun trace(
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        logFormatted(LogLevel.TRACE, format, arg1, arg2)
    }

    override fun trace(
        format: String?,
        vararg arguments: Any?
    ) {
        logFormatted(LogLevel.TRACE, format, *arguments)
    }

    override fun trace(
        msg: String?,
        t: Throwable?
    ) {
        log(LogLevel.TRACE, msg.orEmpty(), t)
    }

    override fun isTraceEnabled(marker: Marker?): Boolean = isTraceEnabled

    override fun trace(
        marker: Marker?,
        msg: String?
    ) {
        trace(msg)
    }

    override fun trace(
        marker: Marker?,
        format: String?,
        arg: Any?
    ) {
        trace(format, arg)
    }

    override fun trace(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        trace(format, arg1, arg2)
    }

    override fun trace(
        marker: Marker?,
        format: String?,
        vararg argArray: Any?
    ) {
        trace(format, *argArray)
    }

    override fun trace(
        marker: Marker?,
        msg: String?,
        t: Throwable?
    ) {
        trace(msg, t)
    }

    override fun isDebugEnabled(): Boolean = isEnabled(LogLevel.DEBUG)
}