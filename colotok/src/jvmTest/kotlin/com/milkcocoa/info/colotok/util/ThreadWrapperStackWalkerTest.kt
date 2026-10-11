package com.milkcocoa.info.colotok.util

import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.logger.eventCallerSnapshot
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class ThreadWrapperStackWalkerTest {
    @Test
    fun reflection_constructor_preserves_visible_reflection_frames() {
        val constructor =
            LogRecord.PlainText::class.java.getConstructor(
                String::class.java,
                String::class.java,
                Level::class.java,
                Map::class.java,
                Instant::class.java
            )
        val record =
            constructor.newInstance("caller", "message", LogLevel.INFO, emptyMap<String, String>(), Clock.System.now())

        assertTrue(record.eventCallerSnapshot.startsWith("j.i.r."), record.eventCallerSnapshot)
    }

    @Test
    fun method_handle_constructor_preserves_the_visible_invocation_frame() {
        val constructor =
            MethodHandles.lookup().findConstructor(
                LogRecord.PlainText::class.java,
                MethodType.methodType(
                    Void.TYPE,
                    String::class.java,
                    String::class.java,
                    Level::class.java,
                    Map::class.java,
                    Instant::class.java
                )
            )
        val record =
            constructor.invokeWithArguments(
                "caller",
                "message",
                LogLevel.INFO,
                emptyMap<String, String>(),
                Clock.System.now()
            ) as LogRecord.PlainText

        assertTrue(
            record.eventCallerSnapshot.contains("MethodHandle#invokeWithArguments:"),
            record.eventCallerSnapshot
        )
    }

    @Test
    fun deep_calls_preserve_the_innermost_user_location() {
        val direct = ConcurrentCallerFixture().createRecord(0)
        val deep = ConcurrentCallerFixture().createRecord(32)

        assertEquals(direct.eventCallerSnapshot, deep.eventCallerSnapshot)
        assertTrue(
            direct.eventCallerSnapshot.contains("ConcurrentCallerFixture#createRecord:"),
            direct.eventCallerSnapshot
        )
    }

    @Test
    fun shared_walker_keeps_each_threads_caller_and_thread_name() {
        val executor = Executors.newFixedThreadPool(4)
        try {
            val tasks =
                List(32) { depth ->
                    Callable {
                        val record = ConcurrentCallerFixture().createRecord(depth)
                        record to Thread.currentThread().name
                    }
                }
            for (future in executor.invokeAll(tasks)) {
                val (record, threadName) = future.get(10, TimeUnit.SECONDS)
                assertEquals(threadName, record.threadName)
                assertTrue(
                    record.eventCallerSnapshot.contains("ConcurrentCallerFixture#createRecord:"),
                    record.eventCallerSnapshot
                )
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private class ConcurrentCallerFixture {
        fun createRecord(depth: Int): LogRecord.PlainText {
            if (depth > 0) return createRecord(depth - 1)
            return LogRecord.PlainText("caller", "message", LogLevel.INFO, emptyMap())
        }
    }
}