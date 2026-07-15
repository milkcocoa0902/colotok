package com.milkcocoa.info.colotok.core.logger

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MDCAndroidScopeContractTest {
    @AfterTest
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun put_then_suspend_retains_value() = runBlocking {
        withContext(MDC.asCoroutineContext()) {
            MDC.put("key", "value")
            yield()
            assertEquals("value", MDC.get("key"))
        }
    }

    @Test
    fun child_inherits_snapshot_and_siblings_are_isolated() = runBlocking {
        withContext(MDC.asCoroutineContext()) {
            MDC.put("key", "parent")
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

    @Test
    fun nested_scope_restores_parent_context() = runBlocking {
        withContext(MDC.asCoroutineContext()) {
            MDC.put("key", "parent")

            withContext(MDC.asCoroutineContext()) {
                MDC.put("key", "child")
                yield()
                assertEquals("child", MDC.get("key"))
            }

            assertEquals("parent", MDC.get("key"))
        }
    }
}
