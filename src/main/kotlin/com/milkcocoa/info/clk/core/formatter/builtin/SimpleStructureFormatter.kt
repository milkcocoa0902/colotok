package com.milkcocoa.info.clk.core.formatter.builtin

import com.milkcocoa.info.clk.core.formatter.Element
import com.milkcocoa.info.clk.core.formatter.details.StructuredFormatter
import javax.crypto.ExemptionMechanism

object SimpleStructureFormatter: StructuredFormatter(listOf(Element.MESSAGE, Element.LEVEL, Element.DATE, Element.TIME))