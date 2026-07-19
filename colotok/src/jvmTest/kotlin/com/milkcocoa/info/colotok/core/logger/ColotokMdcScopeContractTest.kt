package com.milkcocoa.info.colotok.core.logger

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ColotokMdcScopeContractTest {
    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun put_then_suspend_retains_value() = runTest {
        withMdcScope {
            MDC.put("key", "value")

            yield()

            assertEquals("value", MDC.get("key"))
        }
    }

    @Test
    fun child_inherits_creation_snapshot_without_mutating_parent() = runTest {
        withMdcScope {
            MDC.put("key", "creation-value")
            coroutineScope {
                val start = CompletableDeferred<Unit>()
                val child = async {
                    start.await()
                    val inherited = MDC.get("key")
                    MDC.put("key", "child")
                    inherited to MDC.get("key")
                }

                MDC.put("key", "parent-after-creation")
                start.complete(Unit)

                assertEquals("creation-value" to "child", child.await())
                assertEquals("parent-after-creation", MDC.get("key"))
            }
        }
    }

    @Test
    fun siblings_do_not_share_mutations() = runTest {
        withMdcScope {
            MDC.put("key", "parent")
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

    @Test
    fun nested_scope_restores_parent_context() = runTest {
        withMdcScope {
            MDC.put("key", "parent")

            withMdcScope {
                MDC.put("key", "child")
                yield()
                assertEquals("child", MDC.get("key"))
            }

            assertEquals("parent", MDC.get("key"))
        }
    }
}
