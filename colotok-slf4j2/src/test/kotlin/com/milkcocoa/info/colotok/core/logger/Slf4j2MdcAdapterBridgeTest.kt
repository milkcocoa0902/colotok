package com.milkcocoa.info.colotok.core.logger

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class Slf4j2MdcAdapterBridgeTest {

    private val adapter = ColotokMDCAdapter()

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun slf4j_mdc_put_get_affects_colotok_mdc() {
        adapter.put("k", "v")

        assertEquals("v", MDC.get("k"))
        assertEquals("v", adapter.get("k"))

        adapter.remove("k")
        assertNull(MDC.get("k"))
        assertNull(adapter.get("k"))
    }

    @Test
    fun setContextMap_overwrites_threadlocal_context() {
        MDC.put("k", "old")

        val m = mutableMapOf<String, String>()
        m["k"] = "new"
        m["k2"] = "v2"

        @Suppress("UNCHECKED_CAST")
        adapter.setContextMap(m as Map<String?, String?>)

        assertEquals("new", MDC.get("k"))
        assertEquals("v2", MDC.get("k2"))
    }

    @Test
    fun getCopyOfContextMap_returns_copy() {
        MDC.put("k", "v")

        val copy1 = adapter.getCopyOfContextMap()
        assertNotNull(copy1)
        assertEquals("v", copy1["k"])

        // 返却値はスナップショットであること（以降の変更が反映されない）
        MDC.put("k", "mutated")
        val copy2 = adapter.getCopyOfContextMap()
        assertEquals("v", copy1["k"]) // 先に取得したコピーは変わらない
        assertEquals("mutated", copy2["k"]) // 新しいコピーは最新を反映
    }
}
