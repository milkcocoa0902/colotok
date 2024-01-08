package com.milkcocoa.info.colotok.core.formatter.builtin.text

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.formatter.details.TextFormatter

/**
 * builtin formatter to print details log
 *
 * yyyy-MM-ddTHH:mm:ss.SSS+Offset (thread-name)[[Level]] - message, additional = {}
 */
object DetailTextFormatter : TextFormatter(
    """
    ${Element.DATE_TIME} 
    (${Element.THREAD})[${Element.LEVEL}] - ${Element.MESSAGE}, 
    additional = ${Element.ATTR}
    """.trimIndent()
        .replace("\n", "")
)