package com.milkcocoa.info.colotok.core.formatter.builtin.text

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.formatter.details.TextFormatter

/**
 * builtin formatter to print simple log.
 *
 * yyyy-MM-dd HH:mm:ss [[Level]] - message
 */
object SimpleTextFormatter : TextFormatter(
    """
        ${Element.DATE} ${Element.TIME}  
        [${Element.LEVEL}] - ${Element.MESSAGE}
    """.trimIndent()
        .replace("\n", "")
)