package com.milkcocoa.info.colotok.util

import com.milkcocoa.info.colotok.core.logger.LevelScopedLogger
import com.milkcocoa.info.colotok.core.logger.Logger

actual object ThreadWrapper {
    actual fun getCurrentThreadName() = Thread.currentThread().name

    actual fun traceCallPoint() =
        Thread.currentThread().stackTrace
            .asSequence()
            .dropWhile {
                it.className.startsWith(Logger::class.java.name).not() &&
                    it.className.startsWith(LevelScopedLogger::class.java.name).not()
            }
            .dropWhile {
                it.className.startsWith(Logger::class.java.name) ||
                    it.className.startsWith(LevelScopedLogger::class.java.name)
            }
            .firstOrNull()
            ?.let {
                val path = it.className.split(".").dropLast(1).map { it.first() }.joinToString(".")
                val className = it.className.split(".").last()

                "$path.$className#${it.methodName}:${it.lineNumber}"
            } ?: ""
}