package com.milkcocoa.info.colotok.core.formatter.builtin.structure

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.formatter.details.StructuredFormatter

/**
 * builtin formatter to print simple structure log
 *
 * {"msg": "message", "level": "[Level]", "date": "yyyy-MM-dd"}
 */
object SimpleStructureFormatter : StructuredFormatter(
    listOf(
        Element.MESSAGE,
        Element.LEVEL,
        Element.DATE
    ),
    listOf(
        "password",
        "secret"
    )
)