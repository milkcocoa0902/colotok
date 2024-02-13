package com.milkcocoa.info.colotok.core.formatter.details

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.logger.LevelScopedLogger
import com.milkcocoa.info.colotok.core.logger.Logger
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * base class for log formatter which used for text log
 *
 * @constructor
 * @param fmt[String] format string
 */
abstract class TextFormatter(private val fmt: String) : Formatter {
    override fun format(
        msg: String,
        level: Level
    ): String {
        return format(msg, level, mapOf())
    }

    override fun format(
        msg: String,
        level: Level,
        attrs: Map<String, String>
    ): String {
        val dt = ZonedDateTime.now(ZoneId.systemDefault())
        val thread = Thread.currentThread()

        val caller = thread.stackTrace
            .dropWhile {
                it.className.startsWith(Logger::class.java.name).not() &&
                    it.className.startsWith(LevelScopedLogger::class.java.name).not()
            }
            .dropWhile {
                it.className.startsWith(Logger::class.java.name) ||
                    it.className.startsWith(LevelScopedLogger::class.java.name)
            }
            .firstOrNull()?.let {
                val path = it.className.split(".").dropLast(1).map { it.first() }.joinToString(".")
                val className = it.className.split(".").last()

                "${path}.${className}#${it.methodName}:${it.lineNumber}"
            } ?: ""

        return fmt
            .replace(Element.DATE.toString(), dt.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .replace(Element.TIME.toString(), dt.format(DateTimeFormatter.ISO_LOCAL_TIME))
            .replace(Element.DATE_TIME.toString(), dt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            .replace(Element.MESSAGE.toString(), String.format("%s", msg))
            .replace(Element.LEVEL.toString(), String.format("%s", level))
            .replace(Element.THREAD.toString(), String.format("%s", thread.name))
            .replace(Element.ATTR.toString(), attrs.toString())
            .replace(Element.CALLER.toString(), caller)
    }
}