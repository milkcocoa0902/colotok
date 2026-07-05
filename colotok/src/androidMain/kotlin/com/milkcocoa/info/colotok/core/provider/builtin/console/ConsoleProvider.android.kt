package com.milkcocoa.info.colotok.core.provider.builtin.console

import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.Provider

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
public actual class ConsoleProvider actual constructor(config: ConsoleProviderConfig) : Provider(config) {
    constructor(config: ConsoleProviderConfig.() -> Unit) : this(ConsoleProviderConfig().apply(config))

    /**
     * initialize provider with default configuration
     */
    constructor() : this(ConsoleProviderConfig())

    private val isOutputEnabled: Boolean = config.isOutputEnabled
    private val isEnabledForRelease: Boolean = config.isEnabledForRelease
    private val detectDebugModeFn: (() -> Boolean) = config.detectDebugModeFn ?: { false }

    actual override suspend fun onMessage(record: LogRecord) {
        if (!isOutputEnabled) return
        if (!isEnabledForRelease && !detectDebugModeFn.invoke()) return

        runCatching {
            when (record.level) {
                LogLevel.TRACE -> android.util.Log.v(record.name, record.format(config.formatter))
                LogLevel.DEBUG -> android.util.Log.d(record.name, record.format(config.formatter))
                LogLevel.INFO -> android.util.Log.i(record.name, record.format(config.formatter))
                LogLevel.WARN -> android.util.Log.w(record.name, record.format(config.formatter))
                LogLevel.ERROR -> android.util.Log.e(record.name, record.format(config.formatter))
                else -> android.util.Log.d(record.name, record.format(config.formatter))
            }
        }
    }
}