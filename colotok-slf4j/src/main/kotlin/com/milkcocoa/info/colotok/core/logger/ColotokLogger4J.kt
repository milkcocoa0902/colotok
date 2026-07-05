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
        if (throwable == null) {
            delegate.at(level, message)
        } else {
            delegate.at(level, message, mapOf("cause" to throwable.toString()))
        }
    }

    private fun logFormatted(
        level: Level,
        pattern: String?,
        vararg arguments: Any?
    ) {
        val formatted = formatMessage(pattern, *arguments)
        log(level, formatted.message, formatted.throwable)
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

    override fun isInfoEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun info(
        marker: Marker?,
        msg: String?
    ) {
        TODO("Not yet implemented")
    }

    override fun info(
        marker: Marker?,
        format: String?,
        arg: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun info(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun info(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun info(
        marker: Marker?,
        msg: String?,
        t: Throwable?
    ) {
        TODO("Not yet implemented")
    }

    override fun isWarnEnabled(): Boolean {
        TODO("Not yet implemented")
    }

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

    override fun isDebugEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun debug(
        marker: Marker?,
        msg: String?
    ) {
        TODO("Not yet implemented")
    }

    override fun debug(
        marker: Marker?,
        format: String?,
        arg: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun debug(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun debug(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun debug(
        marker: Marker?,
        msg: String?,
        t: Throwable?
    ) {
        TODO("Not yet implemented")
    }

    override fun isInfoEnabled(): Boolean {
        TODO("Not yet implemented")
    }

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

    override fun isWarnEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun warn(
        marker: Marker?,
        msg: String?
    ) {
        TODO("Not yet implemented")
    }

    override fun warn(
        marker: Marker?,
        format: String?,
        arg: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun warn(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun warn(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun warn(
        marker: Marker?,
        msg: String?,
        t: Throwable?
    ) {
        TODO("Not yet implemented")
    }

    override fun isErrorEnabled(): Boolean {
        TODO("Not yet implemented")
    }

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

    override fun isErrorEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun error(
        marker: Marker?,
        msg: String?
    ) {
        TODO("Not yet implemented")
    }

    override fun error(
        marker: Marker?,
        format: String?,
        arg: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun error(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun error(
        marker: Marker?,
        format: String?,
        vararg arguments: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun error(
        marker: Marker?,
        msg: String?,
        t: Throwable?
    ) {
        TODO("Not yet implemented")
    }

    override fun getName(): String = name

    override fun isTraceEnabled(): Boolean {
        TODO("Not yet implemented")
    }

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

    override fun isTraceEnabled(marker: Marker?): Boolean {
        TODO("Not yet implemented")
    }

    override fun trace(
        marker: Marker?,
        msg: String?
    ) {
        TODO("Not yet implemented")
    }

    override fun trace(
        marker: Marker?,
        format: String?,
        arg: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun trace(
        marker: Marker?,
        format: String?,
        arg1: Any?,
        arg2: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun trace(
        marker: Marker?,
        format: String?,
        vararg argArray: Any?
    ) {
        TODO("Not yet implemented")
    }

    override fun trace(
        marker: Marker?,
        msg: String?,
        t: Throwable?
    ) {
        TODO("Not yet implemented")
    }

    override fun isDebugEnabled(): Boolean {
        TODO("Not yet implemented")
    }
}