package com.milkcocoa.info.clk.core.formatter.builtin

import com.milkcocoa.info.clk.core.formatter.Element
import com.milkcocoa.info.clk.core.formatter.details.TextFormatter

object DetailTextFormatter : TextFormatter(
    """
        ${Element.YEAR}/${Element.MONTH}/${Element.DAY} 
        ${Element.HOUR}:${Element.MINUTE}:${Element.SECOND} 
        (${Element.THREAD})[${Element.LEVEL}] - ${Element.MESSAGE}
    """.trimIndent()
        .replace("\n", "")
)