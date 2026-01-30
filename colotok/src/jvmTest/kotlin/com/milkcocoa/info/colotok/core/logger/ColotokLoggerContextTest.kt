package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ColotokLoggerContextTest {

    private class RecordingProvider : Provider() {
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
                is LogRecord.Pin -> {}
            }
        }
    }

    @Test
    fun shallowCopy_copies_providers_and_attrs_snapshot() {
        val provider = RecordingProvider()
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
        val provider = RecordingProvider()
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
        val provider = RecordingProvider()
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
}
