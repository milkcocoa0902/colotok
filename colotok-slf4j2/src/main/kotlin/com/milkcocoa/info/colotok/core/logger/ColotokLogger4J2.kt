package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.level.LogLevel
import org.slf4j.IMarkerFactory
import org.slf4j.MDC
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import org.slf4j.event.Level
import org.slf4j.helpers.AbstractLogger
import org.slf4j.helpers.MessageFormatter
import org.slf4j.spi.MDCAdapter

class ColotokLogger4J2(
    private val name: String,
    private val markerFactory: IMarkerFactory,
    private val mdcAdapter: MDCAdapter
): AbstractLogger() {
    private val delegate: ColotokLogger by lazy {
        ColotokLoggerContext.DEFAULT
            .shallowCopy()
            .putAttrs(mapOf("logger" to name))
            .getLogger(name)
    }

    override fun getFullyQualifiedCallerName(): String = name

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
            delegate.at(colotokLevel, formatted, mapOf("cause" to throwable.toString()))
        }else{
            delegate.at(colotokLevel, formatted)
        }
    }

    override fun isTraceEnabled() = true

    override fun isTraceEnabled(marker: Marker?) = true

    override fun isDebugEnabled() = true

    override fun isDebugEnabled(marker: Marker?) = true

    override fun isInfoEnabled() = true

    override fun isInfoEnabled(marker: Marker?) = true

    override fun isWarnEnabled() = true

    override fun isWarnEnabled(marker: Marker?) = true

    override fun isErrorEnabled(): Boolean = true

    override fun isErrorEnabled(marker: Marker?) = true
}