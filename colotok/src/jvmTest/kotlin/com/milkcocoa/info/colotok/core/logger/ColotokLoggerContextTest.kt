package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlin.test.AfterTest
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import java.util.concurrent.atomic.AtomicInteger

class ColotokLoggerContextTest {
    private val providersToClose = mutableListOf<RecordingProvider>()

    private fun recordingProvider(): RecordingProvider =
        RecordingProvider().also(providersToClose::add)

    @AfterTest
    fun tearDown() {
        providersToClose.forEach { provider ->
            runCatching { provider.forceShutdown() }
        }
        providersToClose.clear()
    }

    @Test
    fun getLogger_caches_instances_by_name() {
        val ctx = ColotokLoggerContext()
        val logger1 = ctx.getLogger("test")
        val logger2 = ctx.getLogger("test")
        val logger3 = ctx.getLogger("other")

        assertSame(logger1, logger2)
        assertTrue(logger1 !== logger3)
    }

    @Test
    fun getLogger_default_name_is_also_cached() {
        val ctx = ColotokLoggerContext()
        val logger1 = ctx.getLogger()
        val logger2 = ctx.getLogger()
        val loggerDefault = ctx.getLogger("Default Logger")

        assertSame(logger1, logger2)
        assertSame(logger1, loggerDefault)
    }

    private class RecordingProvider : Provider(
        config = ConsoleProviderConfig()
    ) {
        data class Record(val name: String, val msg: String, val level: Level, val attr: Map<String, String>)
        val records = mutableListOf<Record>()

        override suspend fun onMessage(record: LogRecord) {
            when(record){
                is LogRecord.PlainText -> {
                    records += Record(record.name, record.msg, record.level, record.attr)
                }
                is LogRecord.StructuredText<*> -> {
                    records += Record(record.name, record.msg.toString(), record.level, record.attr)
                }
                is LogRecord.Metrics -> {
                    records += Record(record.name, record.msg, record.level, record.attr)
                }
                is LogRecord.Pin -> {}
            }
        }
    }

    @Test
    fun shallowCopy_copies_providers_and_attrs_snapshot() {
        val provider = recordingProvider()
        val original = ColotokLoggerContext()
            .addProvider(provider)
            .withAttrs(mapOf("base" to "A"))

        val copy = original.shallowCopy()

        // mutate original attrs after copy
        original.putAttrs(mapOf("mut" to "X"))

        val loggerCopy = copy.getLogger("copy")
        val loggerOrig = original.getLogger("orig")

        loggerCopy.info("hello from copy")
        loggerOrig.info("hello from orig")

        runBlocking { provider.flush() }

        // We expect first record (from copy) to NOT include the mutated key "mut"
        val first = provider.records.first()
        assertEquals("copy", first.name)
        assertEquals(LogLevel.INFO, first.level)
        assertEquals("hello from copy", first.msg)
        assertEquals("A", first.attr["base"])
        assertTrue("mut" !in first.attr)

        val second = provider.records[1]
        assertEquals("orig", second.name)
        assertEquals("X", second.attr["mut"]) // original reflects mutation
    }

    @Test
    fun withAttrs_replaces_attrs_and_freeze_blocks_mutation() {
        val provider = recordingProvider()
        val ctx = ColotokLoggerContext()
            .addProvider(provider)
            .withAttrs(mapOf("k1" to "v1"))

        val logger = ctx.getLogger("L1")
        logger.info("m1")
        runBlocking { provider.flush() }
        val r1 = provider.records.last()
        assertEquals("v1", r1.attr["k1"])

        ctx.freeze()
        assertFailsWith<ColotokLoggerContext.FrozenContextException> {
            ctx.putAttrs(mapOf("k2" to "v2"))
        }
    }

    @Test
    fun putAttrs_merges_with_default_attrs() {
        val provider = recordingProvider()
        val ctx = ColotokLoggerContext()
            .addProvider(provider)
            .withAttrs(mapOf("a" to "1"))

        val logger = ctx.getLogger("L2")
        logger.info("m2", mapOf("b" to "2"))
        runBlocking { provider.flush() }
        val r = provider.records.last()
        // At logging time, attrs passed to logger are merged with context attrs
        assertEquals("1", r.attr["a"])
        assertEquals("2", r.attr["b"])
    }

    @Test
    fun defaultContext_provides_at_least_one_provider() {
        val logger = ColotokLoggerContext.DEFAULT.getLogger("Default")
        // Ensure providers exist and logging does not crash
        assertTrue(logger.providers.isNotEmpty())
        logger.info("ping")
    }

    @Test
    fun context_shutdown_closes_all_providers_before_rethrowing_first_failure() = runBlocking {
        val expected = IllegalStateException("first close failed")
        val secondClosed = AtomicInteger(0)
        val first = object : Provider(ConsoleProviderConfig()) {
            override suspend fun onMessage(record: LogRecord) = Unit
            override fun onClosed() = throw expected
        }
        val second = object : Provider(ConsoleProviderConfig()) {
            override suspend fun onMessage(record: LogRecord) = Unit
            override fun onClosed() { secondClosed.incrementAndGet() }
        }
        val context = ColotokLoggerContext().addProvider(first).addProvider(second)

        assertSame(expected, assertFailsWith<IllegalStateException> { context.shutdown() })
        assertEquals(1, secondClosed.get())
    }

    @Test
    fun context_force_shutdown_closes_registered_providers_without_active_logger() {
        val closed = AtomicInteger(0)
        val provider = object : Provider(ConsoleProviderConfig()) {
            override suspend fun onMessage(record: LogRecord) = Unit
            override fun onClosed() { closed.incrementAndGet() }
        }
        val context = ColotokLoggerContext().addProvider(provider)

        context.forceShutdown()

        assertEquals(1, closed.get())
    }

    @Test
    fun logger_force_shutdown_closes_later_provider_after_first_failure() {
        val expected = IllegalArgumentException("first force close failed")
        val secondClosed = AtomicInteger(0)
        val first = object : Provider(ConsoleProviderConfig()) {
            override suspend fun onMessage(record: LogRecord) = Unit
            override fun onClosed() = throw expected
        }
        val second = object : Provider(ConsoleProviderConfig()) {
            override suspend fun onMessage(record: LogRecord) = Unit
            override fun onClosed() { secondClosed.incrementAndGet() }
        }
        val logger = ColotokLogger("test") { providers = listOf(first, second) }

        assertSame(expected, assertFailsWith<IllegalArgumentException> { logger.forceShutdown() })
        assertEquals(1, secondClosed.get())
    }
}
