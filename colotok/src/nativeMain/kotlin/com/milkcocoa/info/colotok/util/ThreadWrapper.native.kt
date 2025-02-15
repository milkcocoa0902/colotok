package com.milkcocoa.info.colotok.util

import com.milkcocoa.info.colotok.core.logger.LevelScopedLogger
import com.milkcocoa.info.colotok.core.logger.Logger
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.Worker

actual object ThreadWrapper {
    @OptIn(ObsoleteWorkersApi::class)
    actual fun getCurrentThreadName() = Worker.current.name

    @OptIn(ExperimentalNativeApi::class)
    actual fun traceCallPoint() =
        Throwable().getStackTrace().asSequence()
            .dropWhile {
                it.startsWith(Logger::class.simpleName ?: "").not() &&
                    it.startsWith(LevelScopedLogger::class.simpleName ?: "").not()
            }
            .dropWhile {
                it.startsWith(Logger::class.simpleName ?: "") ||
                    it.startsWith(LevelScopedLogger::class.simpleName ?: "")
            }
            .firstOrNull() ?: ""
}