package com.milkcocoa.info.colotok.core.formatter.details

import com.milkcocoa.info.colotok.core.logger.LogLevel
import com.milkcocoa.info.colotok.core.formatter.Element
import kotlinx.serialization.KSerializer
import java.time.LocalDateTime

abstract class TextFormatter(private val fmt: String) : Formatter{
    override fun format(msg: String, level: LogLevel): String {
        val dt = LocalDateTime.now()

        return fmt.replace(Element.YEAR.toString(), String.format("%4d", dt.year))
            .replace(Element.MONTH.toString(), String.format("%02d", dt.monthValue))
            .replace(Element.DAY.toString(), String.format("%02d", dt.dayOfMonth))
            .replace(Element.HOUR.toString(), String.format("%02d", dt.hour))
            .replace(Element.MINUTE.toString(), String.format("%02d", dt.minute))
            .replace(Element.SECOND.toString(), String.format("%02d", dt.second))
            .replace(Element.MESSAGE.toString(), String.format("%s", msg))
            .replace(Element.LEVEL.toString(), String.format("%s", level))
            .replace(Element.THREAD.toString(), String.format("%s", Thread.currentThread().name))
    }

    override fun <T : LogStructure> format(msg: T, serializer: KSerializer<T>, level: LogLevel): String {
        TODO("Not yet implemented")
    }
}