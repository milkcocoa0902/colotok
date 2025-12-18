package com.milkcocoa.info.colotok.core.logger

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ColotokMdcContextElementTest {

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun context_element_switches_and_restores() = runTest {
        MDC.put("k", "outer")
        assertEquals("outer", MDC.get("k"))

        val data = MDC.getThreadLocalContext().deepCopy()
        data.data["k"] = "inner"

        withContext(MDCContext(data)) {
            assertEquals("inner", MDC.get("k"))
        }

        assertEquals("outer", MDC.get("k"))
    }
}
