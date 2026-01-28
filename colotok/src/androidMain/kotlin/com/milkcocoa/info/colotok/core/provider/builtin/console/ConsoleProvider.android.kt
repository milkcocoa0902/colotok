package com.milkcocoa.info.colotok.core.provider.builtin.console

import android.util.Log
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.Provider
import kotlinx.serialization.KSerializer

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
public actual class ConsoleProvider actual constructor(config: ConsoleProviderConfig) : Provider {
    constructor(config: ConsoleProviderConfig.() -> Unit) : this(ConsoleProviderConfig().apply(config))

    /**
     * initialize provider with default configuration
     */
    constructor() : this(ConsoleProviderConfig())

    actual val logLevel: Level = config.level
    actual val formatter: Formatter = config.formatter
    val isEnabledForRelease: Boolean = config.isEnabledForRelease
    val detectDebugModeFn: (() -> Boolean) = config.detectDebugModeFn ?: { false }


    actual override fun write(record: LogRecord) {
        if (isEnabledForRelease.not() and detectDebugModeFn.invoke().not()) return

        if (record.level.isEnabledFor(logLevel).not()) {
            return
        }
        runCatching {
            when(record.level){
                LogLevel.TRACE -> Log.v(record.name, record.format(formatter))
                LogLevel.DEBUG -> Log.d(record.name, record.format(formatter))
                LogLevel.INFO -> Log.i(record.name, record.format(formatter))
                LogLevel.WARN -> Log.w(record.name, record.format(formatter))
                LogLevel.ERROR -> Log.e(record.name, record.format(formatter))
            }
        }
    }
}