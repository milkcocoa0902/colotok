package com.milkcocoa.info.colotok.core.logger

import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ColotokLoggerFactory4JTest {
    @Test
    fun concurrent_requests_for_same_name_return_same_logger_instance() {
        val factory = ColotokLoggerFactory4J()
        val results = ConcurrentLinkedQueue<ColotokLogger4J>()

        runConcurrently(taskCount = 32) {
            results += factory.getLogger("shared")
        }

        assertEquals(32, results.size)
        val first = results.first()
        results.forEach { logger ->
            assertSame(first, logger)
            assertEquals("shared", logger.name)
        }
    }

    @Test
    fun concurrent_requests_for_many_names_complete_without_throwing() {
        val factory = ColotokLoggerFactory4J()
        val results = ConcurrentLinkedQueue<ColotokLogger4J>()

        runConcurrently(taskCount = 64) { index ->
            results += factory.getLogger("logger-$index")
        }

        assertEquals(64, results.size)
        assertEquals((0 until 64).map { "logger-$it" }.toSet(), results.map { it.name }.toSet())
    }

    private fun runConcurrently(
        taskCount: Int,
        action: (Int) -> Unit
    ) {
        val executor = Executors.newFixedThreadPool(taskCount)
        val ready = CountDownLatch(taskCount)
        val start = CountDownLatch(1)

        try {
            val futures =
                (0 until taskCount).map { index ->
                    executor.submit(
                        Callable {
                            ready.countDown()
                            assertTrue(ready.await(5, TimeUnit.SECONDS))
                            assertTrue(start.await(5, TimeUnit.SECONDS))
                            action(index)
                        }
                    )
                }

            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()
            futures.forEach { it.get(5, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }
    }
}