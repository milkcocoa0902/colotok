package com.milkcocoa.info.clk.core.formatter.builtin.structure

import com.milkcocoa.info.clk.core.formatter.Element
import com.milkcocoa.info.clk.core.formatter.details.StructuredFormatter
import javax.crypto.ExemptionMechanism

object DetailStructureFormatter: StructuredFormatter(listOf(Element.MESSAGE, Element.LEVEL, Element.DATE, Element.TIME, Element.THREAD))