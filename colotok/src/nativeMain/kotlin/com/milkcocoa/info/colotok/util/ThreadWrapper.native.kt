package com.milkcocoa.info.colotok.util

import com.milkcocoa.info.colotok.core.logger.LevelScopedColotokLogger
import com.milkcocoa.info.colotok.core.logger.ColotokLogger
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
                it.startsWith(ColotokLogger::class.simpleName ?: "").not() &&
                    it.startsWith(LevelScopedColotokLogger::class.simpleName ?: "").not()
            }
            .dropWhile {
                it.startsWith(ColotokLogger::class.simpleName ?: "") ||
                    it.startsWith(LevelScopedColotokLogger::class.simpleName ?: "")
            }
            .firstOrNull() ?: ""
}