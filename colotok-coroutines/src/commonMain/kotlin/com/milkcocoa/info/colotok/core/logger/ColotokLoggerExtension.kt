package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.writeAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

suspend fun ColotokLogger.atAsync(
    level: Level,
    msg: String
) {
    withContext(Dispatchers.Default) {
        if (attrs.isEmpty()) {
            providers.map {
                async {
                    it.writeAsync(name, msg, level)
                }
            }.awaitAll()
        } else {
            providers.map {
                async {
                    it.writeAsync(name, msg, level, attrs)
                }
            }.awaitAll()
        }
    }
}


suspend fun ColotokLogger.atAsync(
    level: Level,
    msg: String,
    attr: Map<String, String>
) {
    withContext(Dispatchers.Default) {
        providers.map {
            async {
                it.writeAsync(name, msg, level, attr.plus(this@atAsync.attrs))
            }
        }.awaitAll()
    }
}


@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T : LogStructure> ColotokLogger.atAsync(
    level: Level,
    msg: T
) {
    withContext(Dispatchers.Default) {
        if (this@atAsync.attrs.isEmpty()) {
            providers.map {
                async {
                    it.writeAsync(name, msg, T::class.serializer(), level)
                }
            }.awaitAll()
        } else {
            providers.map {
                async {
                    it.writeAsync(name, msg, T::class.serializer(), level, this@atAsync.attrs)
                }
            }.awaitAll()
        }
    }
}

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T : LogStructure> ColotokLogger.atAsync(
    level: Level,
    msg: T,
    attr: Map<String, String>
) {
    withContext(Dispatchers.Default) {
        providers.map {
            async {
                it.writeAsync(name, msg, T::class.serializer(), level, attr.plus(this@atAsync.attrs))
            }
        }.awaitAll()
    }
}

// Level-specific methods for string messages
suspend fun ColotokLogger.traceAsync(msg: String) {
    atAsync(LogLevel.TRACE, msg)
}

suspend fun ColotokLogger.debugAsync(msg: String) {
    atAsync(LogLevel.DEBUG, msg)
}

suspend fun ColotokLogger.infoAsync(msg: String) {
    atAsync(LogLevel.INFO, msg)
}

suspend fun ColotokLogger.warnAsync(msg: String) {
    atAsync(LogLevel.WARN, msg)
}

suspend fun ColotokLogger.errorAsync(msg: String) {
    atAsync(LogLevel.ERROR, msg)
}

// Level-specific methods with attributes
suspend fun ColotokLogger.traceAsync(msg: String, attr: Map<String, String>) {
    atAsync(LogLevel.TRACE, msg, attr)
}

suspend fun ColotokLogger.debugAsync(msg: String, attr: Map<String, String>) {
    atAsync(LogLevel.DEBUG, msg, attr)
}

suspend fun ColotokLogger.infoAsync(msg: String, attr: Map<String, String>) {
    atAsync(LogLevel.INFO, msg, attr)
}

suspend fun ColotokLogger.warnAsync(msg: String, attr: Map<String, String>) {
    atAsync(LogLevel.WARN, msg, attr)
}

suspend fun ColotokLogger.errorAsync(msg: String, attr: Map<String, String>) {
    atAsync(LogLevel.ERROR, msg, attr)
}

// Level-specific methods for LogStructure objects
@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T : LogStructure> ColotokLogger.traceAsync(msg: T) {
    atAsync(LogLevel.TRACE, msg)
}

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T : LogStructure> ColotokLogger.debugAsync(msg: T) {
    atAsync(LogLevel.DEBUG, msg)
}

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T : LogStructure> ColotokLogger.infoAsync(msg: T) {
    atAsync(LogLevel.INFO, msg)
}

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T : LogStructure> ColotokLogger.warnAsync(msg: T) {
    atAsync(LogLevel.WARN, msg)
}

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T : LogStructure> ColotokLogger.errorAsync(msg: T) {
    atAsync(LogLevel.ERROR, msg)
}

// Level-specific methods for LogStructure objects with attributes
@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T : LogStructure> ColotokLogger.traceAsync(msg: T, attr: Map<String, String>) {
    atAsync(LogLevel.TRACE, msg, attr)
}

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T : LogStructure> ColotokLogger.debugAsync(msg: T, attr: Map<String, String>) {
    atAsync(LogLevel.DEBUG, msg, attr)
}

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T : LogStructure> ColotokLogger.infoAsync(msg: T, attr: Map<String, String>) {
    atAsync(LogLevel.INFO, msg, attr)
}

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T : LogStructure> ColotokLogger.warnAsync(msg: T, attr: Map<String, String>) {
    atAsync(LogLevel.WARN, msg, attr)
}

@OptIn(InternalSerializationApi::class)
suspend inline fun <reified T : LogStructure> ColotokLogger.errorAsync(msg: T, attr: Map<String, String>) {
    atAsync(LogLevel.ERROR, msg, attr)
}

// Scoped logging methods
suspend fun ColotokLogger.atTraceAsync(block: suspend LevelScopedColotokLogger.() -> Unit) {
    atAsync(LogLevel.TRACE, block)
}

suspend fun ColotokLogger.atDebugAsync(block: suspend LevelScopedColotokLogger.() -> Unit) {
    atAsync(LogLevel.DEBUG, block)
}

suspend fun ColotokLogger.atInfoAsync(block: suspend LevelScopedColotokLogger.() -> Unit) {
    atAsync(LogLevel.INFO, block)
}

suspend fun ColotokLogger.atWarnAsync(block: suspend LevelScopedColotokLogger.() -> Unit) {
    atAsync(LogLevel.WARN, block)
}

suspend fun ColotokLogger.atErrorAsync(block: suspend LevelScopedColotokLogger.() -> Unit) {
    atAsync(LogLevel.ERROR, block)
}

suspend fun ColotokLogger.atAsync(level: Level, block: suspend LevelScopedColotokLogger.() -> Unit) {
    val scopedLogger = LevelScopedColotokLogger(
        name = name,
        config = ColotokConfig().apply {
            this.providers = this@atAsync.providers
            this.defaultAttrs = this@atAsync.attrs
        },
        level = level
    )
    scopedLogger.block()
}
