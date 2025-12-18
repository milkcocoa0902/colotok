package com.milkcocoa.info.colotok.core.logger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ColotokMdcIsolationTest {

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun parallel_scopes_do_not_leak() = runTest {
        val a = async {
            val data = MDCContextData()
            data.data["k"] = "A"
            withContext(MDCContext(data)) { MDC.get("k") }
        }

        val b = async(Dispatchers.Default) {
            val data = MDCContextData()
            data.data["k"] = "B"
            withContext(MDCContext(data)) { MDC.get("k") }
        }

        assertEquals("A", a.await())
        assertEquals("B", b.await())
    }
}
