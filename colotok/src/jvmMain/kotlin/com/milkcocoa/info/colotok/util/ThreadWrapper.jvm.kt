package com.milkcocoa.info.colotok.util

import com.milkcocoa.info.colotok.core.logger.ColotokLogger
import com.milkcocoa.info.colotok.core.logger.LevelScopedColotokLogger
import com.milkcocoa.info.colotok.core.logger.LogEventMetadata
import com.milkcocoa.info.colotok.core.logger.LogRecord

actual object ThreadWrapper {
    private val loggingWrapperClassNames =
        setOf(
            "com.milkcocoa.info.colotok.core.logger.ColotokLoggerExtensionKt",
            "com.milkcocoa.info.colotok.core.logger.ColotokLogger4J",
            "com.milkcocoa.info.colotok.core.logger.ColotokLogger4J2"
        )

    actual fun getCurrentThreadName() = Thread.currentThread().name

    actual fun traceCallPoint() =
        Thread.currentThread().stackTrace
            .asSequence()
            .dropWhile {
                it.isLoggingFrame().not()
            }
            .dropWhile {
                it.isLoggingFrame()
            }
            .firstOrNull()
            ?.let {
                val path = it.className.split(".").dropLast(1).map { it.first() }.joinToString(".")
                val className = it.className.split(".").last()

                "$path.$className#${it.methodName}:${it.lineNumber}"
            } ?: ""

    private fun StackTraceElement.isLoggingFrame(): Boolean =
        isClassOrNestedClass(ThreadWrapper::class.java.name) ||
            isClassOrNestedClass(LogEventMetadata::class.java.name) ||
            isClassOrNestedClass(LogRecord::class.java.name) ||
            isClassOrNestedClass(ColotokLogger::class.java.name) ||
            isClassOrNestedClass(LevelScopedColotokLogger::class.java.name) ||
            loggingWrapperClassNames.any { isClassOrNestedClass(it) } ||
            className.startsWith("org.slf4j.")

    private fun StackTraceElement.isClassOrNestedClass(ownerName: String): Boolean =
        className == ownerName || className.startsWith("$ownerName$")
}