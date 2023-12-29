package com.milkcocoa.info.colotok.core.formatter.builtin.structure

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.formatter.details.StructuredFormatter


/**
 * builtin formatter to print structure log for more details
 *
 * {"msg": "message", "level": "[LogLevel]", "date": "yyyy-MM-ddTHH:mm:ss.SSS+Offset", "thread": "thread-name", ....(additional attrs)}
 */
object DetailStructureFormatter: StructuredFormatter(
    listOf(
        Element.MESSAGE,
        Element.LEVEL,
        Element.DATE_TIME,
        Element.THREAD,
        Element.ATTR)
)