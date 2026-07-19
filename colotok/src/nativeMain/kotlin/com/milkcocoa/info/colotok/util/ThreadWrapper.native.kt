package com.milkcocoa.info.colotok.util

import com.milkcocoa.info.colotok.core.logger.ColotokLogger
import com.milkcocoa.info.colotok.core.logger.LevelScopedColotokLogger
import com.milkcocoa.info.colotok.core.logger.LogEventMetadata
import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.concurrent.ObsoleteWorkersApi
import kotlin.native.concurrent.Worker

actual object ThreadWrapper {
    @OptIn(ObsoleteWorkersApi::class)
    actual fun getCurrentThreadName() = Worker.current.name

    @OptIn(ExperimentalNativeApi::class)
    actual fun traceCallPoint(): String {
        val loggingFrameNames = listOfNotNull(
            ThreadWrapper::class.simpleName,
            LogEventMetadata::class.simpleName,
            LogRecord::class.simpleName,
            ColotokLogger::class.simpleName,
            LevelScopedColotokLogger::class.simpleName,
            "ColotokLoggerExtensionKt",
        )

        return Throwable().getStackTrace().asSequence()
            .dropWhile {
                frame -> loggingFrameNames.none { frame.containsLoggingOwner(it) }
            }
            .dropWhile {
                frame -> loggingFrameNames.any { frame.containsLoggingOwner(it) }
            }
            .firstOrNull() ?: ""
    }

    private fun String.containsLoggingOwner(ownerName: String): Boolean =
        contains("$ownerName.") || contains("$ownerName#")
}
