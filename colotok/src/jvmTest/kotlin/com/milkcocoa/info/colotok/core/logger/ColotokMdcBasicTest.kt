package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.level.LogLevel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ColotokMdcBasicTest {
    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun put_get_remove_clear() {
        assertNull(MDC.get("k"))

        MDC.put("k", "v")
        assertEquals("v", MDC.get("k"))

        MDC.remove("k")
        assertNull(MDC.get("k"))

        MDC.put("k2", "v2")
        MDC.clear()
        assertNull(MDC.get("k2"))
    }

    @Test
    fun logRecord_keeps_mdc_snapshot_after_context_mutation() {
        MDC.put("request_id", "before")

        val record = LogRecord.PlainText("test", "message", LogLevel.INFO, emptyMap())
        MDC.put("request_id", "after")

        assertEquals("before", record.mdcContextDataSnapshot.data["request_id"])
        assertEquals("after", MDC.get("request_id"))
    }
}