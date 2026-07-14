package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.level.Level as ColotokLevel
import org.slf4j.IMarkerFactory
import org.slf4j.MDC
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.AbstractLogger
import org.slf4j.helpers.MessageFormatter
import org.slf4j.spi.MDCAdapter

class ColotokLogger4J2(
    private val loggerName: String,
    private val markerFactory: IMarkerFactory,
    private val mdcAdapter: MDCAdapter
): AbstractLogger() {
    private val delegate: ColotokLogger by lazy {
        ColotokLoggerContext.DEFAULT
            .shallowCopy()
            .putAttrs(mapOf("logger" to loggerName))
            .getLogger(loggerName)
    }

    override fun getName(): String = loggerName

    override fun getFullyQualifiedCallerName(): String = loggerName

    override fun handleNormalizedLoggingCall(
        level: Level?,
        marker: Marker?,
        messagePattern: String?,
        arguments: Array<out Any?>?,
        throwable: Throwable?
    ) {
        val safePattern = messagePattern ?: ""
        val formatted = if(arguments == null) {
            safePattern
        }else{
            MessageFormatter.arrayFormat(safePattern, arguments).message
        }

        val colotokLevel = when(level){
            Level.TRACE -> LogLevel.TRACE
            Level.DEBUG -> LogLevel.DEBUG
            Level.INFO -> LogLevel.INFO
            Level.WARN -> LogLevel.WARN
            Level.ERROR -> LogLevel.ERROR
            else -> null
        } ?: return
        MDC.getCopyOfContextMap().forEach { k, v ->
            com.milkcocoa.info.colotok.core.logger.MDC.put(k, v)
        }

        if(throwable != null){
            delegate.at(colotokLevel, formatted, mapOf("cause" to throwable.stackTraceToString()))
        }else{
            delegate.at(colotokLevel, formatted)
        }
    }

    private fun isEnabled(level: ColotokLevel): Boolean =
        delegate.providers.any { level.isEnabledFor(it.config.level) }

    override fun isTraceEnabled() = isEnabled(LogLevel.TRACE)

    override fun isTraceEnabled(marker: Marker?) = isTraceEnabled

    override fun isDebugEnabled() = isEnabled(LogLevel.DEBUG)

    override fun isDebugEnabled(marker: Marker?) = isDebugEnabled

    override fun isInfoEnabled() = isEnabled(LogLevel.INFO)

    override fun isInfoEnabled(marker: Marker?) = isInfoEnabled

    override fun isWarnEnabled() = isEnabled(LogLevel.WARN)

    override fun isWarnEnabled(marker: Marker?) = isWarnEnabled

    override fun isErrorEnabled(): Boolean = isEnabled(LogLevel.ERROR)

    override fun isErrorEnabled(marker: Marker?) = isErrorEnabled
}
