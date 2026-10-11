package com.milkcocoa.info.colotok.util

import com.milkcocoa.info.colotok.core.logger.ColotokLogger
import com.milkcocoa.info.colotok.core.logger.LevelScopedColotokLogger
import com.milkcocoa.info.colotok.core.logger.LogEventMetadata
import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.Worker

actual object ThreadWrapper {
    private val loggingFramePatterns =
        listOfNotNull(
            ThreadWrapper::class.simpleName,
            LogEventMetadata::class.simpleName,
            LogRecord::class.simpleName,
            ColotokLogger::class.simpleName,
            LevelScopedColotokLogger::class.simpleName,
            "ColotokLoggerExtensionKt"
        ).flatMap { listOf("$it.", "$it#") }.toTypedArray()

    @OptIn(ObsoleteWorkersApi::class)
    actual fun getCurrentThreadName() = Worker.current.name

    @OptIn(ExperimentalNativeApi::class)
    actual fun traceCallPoint(): String = firstCallerFrame(Throwable().getStackTrace()) { it.isLoggingFrame() } ?: ""

    private fun String.isLoggingFrame(): Boolean {
        for (pattern in loggingFramePatterns) {
            if (contains(pattern)) return true
        }
        return false
    }
}