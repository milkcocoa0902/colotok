package com.milkcocoa.info.colotok.util

import com.milkcocoa.info.colotok.core.logger.LevelScopedColotokLogger
import com.milkcocoa.info.colotok.core.logger.ColotokLogger

actual object ThreadWrapper {
    actual fun getCurrentThreadName() = Thread.currentThread().name

    actual fun traceCallPoint() =
        Thread.currentThread().stackTrace
            .asSequence()
            .dropWhile {
                it.className.startsWith(ColotokLogger::class.java.name).not() &&
                    it.className.startsWith(LevelScopedColotokLogger::class.java.name).not()
            }
            .dropWhile {
                it.className.startsWith(ColotokLogger::class.java.name) ||
                    it.className.startsWith(LevelScopedColotokLogger::class.java.name)
            }
            .firstOrNull()
            ?.let {
                val path = it.className.split(".").dropLast(1).map { it.first() }.joinToString(".")
                val className = it.className.split(".").last()

                "$path.$className#${it.methodName}:${it.lineNumber}"
            } ?: ""
}