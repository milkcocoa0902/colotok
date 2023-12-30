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
import com.milkcocoa.info.colotok.util.unit.Size.KiB
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

        /**
         * size of buffer in Byte. which is used for save i/o.
         */
        var bufferSize = 4.KiB()

        /**
         * use buffering. if enable this, provider does not write to file until buffered data exceed `bufferSize`. if false write data into file immediately.
         */
        var enableBuffer = true

        var outputStreamBuilder: (()->OutputStream) = { OutputStream.nullOutputStream() }
    }

    private val logLevel = config.logLevel
    private val formatter = config.formatter
    private val outputStream = config.outputStreamBuilder

    private val enableBuffer = config.enableBuffer
    private val bufferSize = config.bufferSize


    private val sb: StringBuilder = StringBuilder()
    override fun write(name: String, msg: String, level: LogLevel, attr: Map<String, String>) {
        if(level.isEnabledFor(logLevel).not()){
            return
        }

        runCatching {
            if(enableBuffer){
                sb.appendLine(formatter.format(msg, level, attr))
                if(sb.length > bufferSize){
                    outputStream().buffered(64.KiB()).use { bos ->
                        bos.write(sb.toString().encodeToByteArray())
                        sb.clear()
                    }
                }
            }else{
                outputStream().buffered(64.KiB()).use { bos ->
                    bos.write(formatter.format(msg.plus("\n"), level, attr).encodeToByteArray())
                }
            }
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

        runCatching {
            if(enableBuffer){
                sb.appendLine(formatter.format(msg, serializer, level, attr))
                if(sb.length > bufferSize){
                    outputStream().buffered(64.KiB()).use { bos ->
                        bos.write(sb.toString().encodeToByteArray())
                        sb.clear()
                    }
                }
            }else{
                outputStream().buffered(64.KiB()).use { bos ->
                    bos.write(formatter.format(msg, serializer, level, attr).plus("\n").encodeToByteArray())
                }
            }
        }
    }

    /**
     * flush buffered data into file.
     */
    fun flush(){
        outputStream().buffered(64.KiB()).use { bos ->
            bos.write(sb.toString().encodeToByteArray())
            sb.clear()
        }
    }
}