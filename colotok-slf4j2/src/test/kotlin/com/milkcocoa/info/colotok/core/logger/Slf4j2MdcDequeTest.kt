package com.milkcocoa.info.colotok.core.logger

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class Slf4j2MdcDequeTest {

    private val adapter = ColotokMDCAdapter()

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun push_pop_behaves_like_stack_and_updates_single_value() {
        adapter.pushByKey("k", "A")
        assertEquals("A", MDC.get("k"))

        adapter.pushByKey("k", "B")
        assertEquals("B", MDC.get("k"))

        val popped1 = adapter.popByKey("k")
        assertEquals("B", popped1)
        assertEquals("A", MDC.get("k"))

        val popped2 = adapter.popByKey("k")
        assertEquals("A", popped2)
        assertNull(MDC.get("k"))
    }

    @Test
    fun getCopyOfDequeByKey_returns_copy() {
        adapter.pushByKey("k", "A")
        adapter.pushByKey("k", "B")

        val deque = adapter.getCopyOfDequeByKey("k")
        assertNotNull(deque)
        assertEquals("B", deque!!.peekFirst())

        deque.removeFirst()
        // copy を壊しても本体は壊れない
        assertEquals("B", MDC.get("k"))
    }

    @Test
    fun clearDequeByKey_clears_and_removes_value() {
        adapter.pushByKey("k", "A")
        adapter.pushByKey("k", "B")

        adapter.clearDequeByKey("k")

        assertNull(MDC.get("k"))
        assertNull(adapter.getCopyOfDequeByKey("k"))
    }
}
