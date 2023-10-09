package com.milkcocoa.info.clk.core.formatter.details

import com.milkcocoa.info.clk.core.LogLevel
import com.milkcocoa.info.clk.core.formatter.Element
import java.time.LocalDateTime

abstract class Formatter(private val fmt: String) {
    fun format(msg: String, level: LogLevel): String {
        val dt = LocalDateTime.now()

        return fmt.replace(Element.YEAR.toString(), String.format("%4d", dt.year))
            .replace(Element.MONTH.toString(), String.format("%02d", (dt.monthValue + 1)))
            .replace(Element.DAY.toString(), String.format("%02d", dt.dayOfMonth))
            .replace(Element.HOUR.toString(), String.format("%02d", dt.hour))
            .replace(Element.MINUTE.toString(), String.format("%02d", dt.minute))
            .replace(Element.SECOND.toString(), String.format("%02d", dt.second))
            .replace(Element.MESSAGE.toString(), String.format("%s", msg))
            .replace(Element.LEVEL.toString(), String.format("%s", level))
            .replace(Element.THREAD.toString(), String.format("%s", Thread.currentThread().name))
    }
}