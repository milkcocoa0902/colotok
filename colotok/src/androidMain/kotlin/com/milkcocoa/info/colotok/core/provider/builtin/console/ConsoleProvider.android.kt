package com.milkcocoa.info.colotok.core.provider.builtin.console

import android.util.Log
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
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

    actual override fun write(
        name: String,
        msg: String,
        level: Level,
        attr: Map<String, String>
    ) {
        if (isEnabledForRelease.not() and detectDebugModeFn.invoke().not()) return

        if (level.isEnabledFor(logLevel).not()) {
            return
        }
        when (level) {
            LogLevel.TRACE -> Log.v(name, formatter.format(msg, level, attr))
            LogLevel.DEBUG -> Log.d(name, formatter.format(msg, level, attr))
            LogLevel.INFO -> Log.i(name, formatter.format(msg, level, attr))
            LogLevel.WARN -> Log.w(name, formatter.format(msg, level, attr))
            LogLevel.ERROR -> Log.e(name, formatter.format(msg, level, attr))
            else -> Log.println(level.levelInt, name, formatter.format(msg, level, attr))
        }
    }

    actual override fun <T : LogStructure> write(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attr: Map<String, String>
    ) {
        if (isEnabledForRelease.not() and detectDebugModeFn.invoke().not()) return

        if (level.isEnabledFor(logLevel).not()) {
            return
        }
        when (level) {
            LogLevel.TRACE -> Log.v(name, formatter.format(msg, serializer, level, attr))
            LogLevel.DEBUG -> Log.d(name, formatter.format(msg, serializer, level, attr))
            LogLevel.INFO -> Log.i(name, formatter.format(msg, serializer, level, attr))
            LogLevel.WARN -> Log.w(name, formatter.format(msg, serializer, level, attr))
            LogLevel.ERROR -> Log.e(name, formatter.format(msg, serializer, level, attr))
            else -> Log.println(level.levelInt, name, formatter.format(msg, serializer, level, attr))
        }
    }
}