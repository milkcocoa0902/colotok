package com.milkcocoa.info.colotok.core.formatter.builtin.structure

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.formatter.details.StructuredFormatter

object DetailStructureFormatter: StructuredFormatter(
    listOf(
        Element.MESSAGE,
        Element.LEVEL,
        Element.DATE_TIME,
        Element.THREAD)
)