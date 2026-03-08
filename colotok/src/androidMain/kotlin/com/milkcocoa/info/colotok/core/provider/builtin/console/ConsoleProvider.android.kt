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
public actual class ConsoleProvider actual constructor(config: ConsoleProviderConfig) : Provider(config) {
    constructor(config: ConsoleProviderConfig.() -> Unit) : this(ConsoleProviderConfig().apply(config))

    /**
     * initialize provider with default configuration
     */
    constructor() : this(ConsoleProviderConfig())

    private val isEnabledForRelease: Boolean = config.isEnabledForRelease
    private val detectDebugModeFn: (() -> Boolean) = config.detectDebugModeFn ?: { false }


    actual override suspend fun onMessage(record: LogRecord) {
        if (isEnabledForRelease.not() and detectDebugModeFn.invoke().not()) return

        runCatching {
            when(record.level){
                LogLevel.TRACE -> Log.v(record.name, record.format(config.formatter))
                LogLevel.DEBUG -> Log.d(record.name, record.format(config.formatter))
                LogLevel.INFO -> Log.i(record.name, record.format(config.formatter))
                LogLevel.WARN -> Log.w(record.name, record.format(config.formatter))
                LogLevel.ERROR -> Log.e(record.name, record.format(config.formatter))
                else -> Log.d(record.name, record.format(config.formatter))
            }
        }
    }
}