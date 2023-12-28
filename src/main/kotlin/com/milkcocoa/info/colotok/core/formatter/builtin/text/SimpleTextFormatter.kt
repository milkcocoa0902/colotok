package com.milkcocoa.info.colotok.core.formatter.builtin.text

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.formatter.details.TextFormatter

object SimpleTextFormatter : TextFormatter(
    """
        ${Element.YEAR}/${Element.MONTH}/${Element.DAY} 
        ${Element.HOUR}:${Element.MINUTE}:${Element.SECOND} 
        [${Element.LEVEL}] - ${Element.MESSAGE}
    """.trimIndent()
        .replace("\n", "")
)