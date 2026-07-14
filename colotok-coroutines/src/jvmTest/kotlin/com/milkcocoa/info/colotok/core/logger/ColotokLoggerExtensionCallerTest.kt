package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.formatter.details.TextFormatter
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ColotokLoggerExtensionCallerTest {
    @Test
    fun async_logger_captures_the_user_call_site() = runTest {
        val provider = ColotokLoggerExtensionTest.TestAsyncProvider()
        val logger = ColotokLogger("test-logger") {
            providers = listOf(provider)
        }

        logger.infoAsync("message")
        provider.flush()
        val formatted = object : TextFormatter("${Element.CALLER}") {}.format(provider.lastRecord!!)

        assertTrue(formatted.contains("async_logger_captures_the_user_call_site"), formatted)
        provider.join()
    }
}
