package com.milkcocoa.info.clk.core.formatter.builtin.text

import com.milkcocoa.info.clk.core.formatter.Element
import com.milkcocoa.info.clk.core.formatter.details.TextFormatter

object SimpleTextFormatter : TextFormatter(
    """
        ${Element.YEAR}/${Element.MONTH}/${Element.DAY} 
        ${Element.HOUR}:${Element.MINUTE}:${Element.SECOND} 
        [${Element.LEVEL}] - ${Element.MESSAGE}
    """.trimIndent()
        .replace("\n", "")
)