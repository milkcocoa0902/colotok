package com.milkcocoa.info.colotok.core.logger

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MDCContractTest {
    @AfterTest
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun clear_removes_map_and_deque() {
        val context = MDCContextData(
            data = mutableMapOf("key" to "value"),
            dequeData = mutableMapOf("key" to ArrayDeque(listOf("first", "second")))
        )
        MDC.setThreadLocalContext(context)

        MDC.clear()

        assertNull(MDC.get("key"))
        assertEquals(emptyMap(), MDC.getThreadLocalContext().dequeData)
    }

    @Test
    fun set_context_replaces_map_and_deque() {
        MDC.setThreadLocalContext(
            MDCContextData(
                data = mutableMapOf("old" to "value"),
                dequeData = mutableMapOf("old" to ArrayDeque(listOf("value")))
            )
        )

        MDC.setThreadLocalContext(
            MDCContextData(
                data = mutableMapOf("new" to "value"),
                dequeData = mutableMapOf("new" to ArrayDeque(listOf("value")))
            )
        )

        val actual = MDC.getThreadLocalContext()
        assertEquals(mapOf("new" to "value"), actual.data)
        assertEquals(setOf("new"), actual.dequeData.keys)
        assertEquals(listOf("value"), actual.dequeData.getValue("new").toList())
    }
}
