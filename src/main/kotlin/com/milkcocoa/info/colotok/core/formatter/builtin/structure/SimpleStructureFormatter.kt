package com.milkcocoa.info.colotok.core.formatter.builtin.structure

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.formatter.details.StructuredFormatter

object SimpleStructureFormatter: StructuredFormatter(listOf(Element.MESSAGE, Element.LEVEL, Element.DATE, Element.TIME))