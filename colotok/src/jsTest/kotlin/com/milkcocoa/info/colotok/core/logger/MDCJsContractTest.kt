package com.milkcocoa.info.colotok.core.logger

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.promise
import kotlinx.coroutines.yield
import kotlin.test.AfterTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MDCJsContractTest {
    @AfterTest
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun js_root_context_operations_persist_after_call() {
        MDC.put("put", "value")
        assertEquals("value", MDC.get("put"))

        MDC.setThreadLocalContext(MDCContextData(mutableMapOf("set" to "value")))
        assertNull(MDC.get("put"))
        assertEquals("value", MDC.get("set"))

        MDC.clear()
        assertNull(MDC.get("set"))
    }

    @Test
    fun nested_scope_restores_parent_context() {
        MDC.put("key", "parent")

        withMdcScope {
            assertEquals("parent", MDC.get("key"))
            MDC.put("key", "child")
            assertEquals("child", MDC.get("key"))
        }

        assertEquals("parent", MDC.get("key"))
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun put_then_await_retains_value() = withMdcScope {
        GlobalScope.promise {
            MDC.put("key", "value")
            yield()
            assertEquals("value", MDC.get("key"))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Ignore
    @Test
    fun child_inherits_creation_snapshot_and_siblings_are_isolated() = withMdcScope {
        MDC.put("key", "parent")
        GlobalScope.promise {
            coroutineScope {
                val firstReady = CompletableDeferred<Unit>()
                val secondReady = CompletableDeferred<Unit>()
                val first = async {
                    MDC.put("key", "first")
                    firstReady.complete(Unit)
                    secondReady.await()
                    MDC.get("key")
                }
                val second = async {
                    firstReady.await()
                    MDC.put("key", "second")
                    secondReady.complete(Unit)
                    MDC.get("key")
                }

                assertEquals("first", first.await())
                assertEquals("second", second.await())
                assertEquals("parent", MDC.get("key"))
            }
        }
    }
}
