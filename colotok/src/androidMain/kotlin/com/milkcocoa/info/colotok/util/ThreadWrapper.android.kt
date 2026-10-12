package com.milkcocoa.info.colotok.util

import com.milkcocoa.info.colotok.core.logger.ColotokLogger
import com.milkcocoa.info.colotok.core.logger.LevelScopedColotokLogger
import com.milkcocoa.info.colotok.core.logger.LogEventMetadata
import com.milkcocoa.info.colotok.core.logger.LogRecord

actual object ThreadWrapper {
    private val loggingClassNames =
        arrayOf(
            ThreadWrapper::class.java.name,
            LogEventMetadata::class.java.name,
            LogRecord::class.java.name,
            ColotokLogger::class.java.name,
            LevelScopedColotokLogger::class.java.name,
            "com.milkcocoa.info.colotok.core.logger.ColotokLoggerExtensionKt",
            "com.milkcocoa.info.colotok.core.logger.ColotokLogger4J",
            "com.milkcocoa.info.colotok.core.logger.ColotokLogger4J2"
        )

    private val loggingNestedClassPrefixes = Array(loggingClassNames.size) { loggingClassNames[it] + '$' }

    actual fun getCurrentThreadName() = Thread.currentThread().name

    actual fun traceCallPoint(): String {
        val caller = firstCallerFrame(Thread.currentThread().stackTrace) { it.isLoggingFrame() } ?: return ""
        return formatCallerFrame(caller.className, caller.methodName, caller.lineNumber)
    }

    private fun StackTraceElement.isLoggingFrame(): Boolean {
        val name = className
        for (index in loggingClassNames.indices) {
            if (name == loggingClassNames[index] || name.startsWith(loggingNestedClassPrefixes[index])) return true
        }
        return name.startsWith("org.slf4j.")
    }
}