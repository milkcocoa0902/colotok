package com.milkcocoa.info.colotok.core.formatter.builtin.text

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.formatter.details.TextFormatter

/**
 * builtin formatter to print only message
 */
object PlainTextFormatter : TextFormatter(
    "${Element.MESSAGE}"
)