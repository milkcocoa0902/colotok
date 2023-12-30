package com.milkcocoa.info.colotok.core.provider.builtin

import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.logger.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.core.provider.details.ProviderColorConfig
import com.milkcocoa.info.colotok.core.provider.details.ProviderConfig
import com.milkcocoa.info.colotok.util.color.AnsiColor
import com.milkcocoa.info.colotok.util.color.Color
import com.milkcocoa.info.colotok.util.color.ColorExtension.red
import kotlinx.serialization.KSerializer
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.encoding.Base64.Default.encodeToByteArray

class StreamProvider(config: StreamProviderConfig): Provider {

    constructor(config: StreamProviderConfig.() -> Unit): this(StreamProviderConfig().apply(config))
    constructor(): this(StreamProviderConfig())

    class StreamProviderConfig: ProviderConfig{
        override var logLevel: LogLevel = LogLevel.DEBUG
        override var formatter: Formatter = DetailTextFormatter
        override var colorize: Boolean = false

        var outputStreamBuilder: (()->OutputStream) = { OutputStream.nullOutputStream() }
    }

    private val logLevel = config.logLevel
    private val formatter = config.formatter
    private val colorize = config.colorize
    private val outputStream = config.outputStreamBuilder

    override fun write(name: String, msg: String, level: LogLevel, attr: Map<String, String>) {
        if(level.isEnabledFor(logLevel).not()){
            return
        }

        outputStream().use { os ->
            kotlin.runCatching {
                os.write(formatter.format(msg, level, attr).plus("\n").encodeToByteArray())
                os.flush()
            }.getOrElse { println(it.message?.red()) }
        }
    }

    override fun <T : LogStructure> write(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: LogLevel,
        attr: Map<String, String>
    ) {
        if(level.isEnabledFor(logLevel).not()){
            return
        }

        outputStream().use { os ->
            kotlin.runCatching {
                os.write(formatter.format(msg, serializer, level, attr).plus("\n").encodeToByteArray())
                os.flush()
            }.getOrElse { println(it.message?.red()) }
        }
    }
}