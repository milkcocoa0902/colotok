package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.TextFormatter
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProvider
import kotlin.test.Test

class LoggerTest {
    @Test
    fun test_00001() {
        val logger =
            LoggerFactory()
                .addProvider(
                    ConsoleProvider {
                        formatter = DetailTextFormatter
                        level = LogLevel.INFO
                    }
                )
                .getLogger()
        logger.info("test_00001")
    }

    @Test
    fun test_00002() {
        val logger =
            LoggerFactory()
                .addProvider(
                    ConsoleProvider {
                        formatter =
                            object : TextFormatter(
                                """
                                %{TEST}
                                ${Element.DATETIME} 
                                (${Element.THREAD}) - ${Element.MESSAGE}, 
                                additional = ${Element.ATTR}
                                """.trimIndent()
                                    .replace("\n", "")
                            ) {}
                        level = LogLevel.INFO
                    }
                )
                .getLogger()

        withMdcScope {
            MDC.put("TEST", "test")
            logger.info("test_00001")
        }

        logger.info("test_00002")
    }

    @Test
    fun test_00003() {
    }
}