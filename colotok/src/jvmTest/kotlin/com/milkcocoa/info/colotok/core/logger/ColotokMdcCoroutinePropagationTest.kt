package com.milkcocoa.info.colotok.core.logger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ColotokMdcCoroutinePropagationTest {

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun mdc_is_preserved_inside_scope_even_if_thread_changes() = runTest {
        MDC.put("k", "base")

        val data = MDC.getThreadLocalContext().deepCopy()
        data.data["k"] = "scoped"

        withContext(MDCContext(data)) {
            val d1 = async(Dispatchers.Default) { MDC.get("k") }
            val d2 = async(Dispatchers.Default) { MDC.get("k") }

            assertEquals("scoped", d1.await())
            assertEquals("scoped", d2.await())
        }

        assertEquals("base", MDC.get("k"))
    }
}
