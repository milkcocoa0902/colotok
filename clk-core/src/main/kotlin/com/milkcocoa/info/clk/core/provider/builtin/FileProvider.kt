package com.milkcocoa.info.clk.core.provider.builtin

import com.milkcocoa.info.clk.core.LogLevel
import com.milkcocoa.info.clk.core.formatter.builtin.DetailFormatter
import com.milkcocoa.info.clk.core.formatter.details.Formatter
import com.milkcocoa.info.clk.core.provider.details.Provider
import com.milkcocoa.info.clk.core.provider.details.ProviderConfig
import com.milkcocoa.info.clk.core.provider.rotation.Rotation
import com.milkcocoa.info.clk.util.color.AnsiColor
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
class FileProvider(val filename: String, config: FileProviderConfig) : Provider {

    /**
     * initialize provider with default configuration
     */
    constructor(filename: String): this(filename, FileProviderConfig())

    /**
     * initialize provider with specified configuration
     */
    constructor(filename: String, config: FileProviderConfig.() -> Unit): this(filename, FileProviderConfig().apply(config))
    class FileProviderConfig() : ProviderConfig{
        /**
         * size of buffer in Byte
         */
        var bufferSize = 1024

        /**
         * use buffering. if enable this, provider does not write to file until buffered data exceed `bufferSize`
         */
        var enableBuffer = true

        /**
         * if not null, rotate after write
         */
        var rotation: Rotation? = null

        override var logLevel: LogLevel = LogLevel.DEBUG

        override var formatter: Formatter = DetailFormatter

        /**
         * no effect
         */
        override var colorize: Boolean = false
    }

    private val enableBuffer = config.enableBuffer
    private val bufferSize = config.bufferSize
    private val rotation = config.rotation
    private val logLevel = config.logLevel
    private val formatter = config.formatter

    private val sb: StringBuilder = StringBuilder()
    override fun write(name: String, str: String, level: LogLevel) {
        if(level.isEnabledFor(logLevel).not()){
            return
        }
        runCatching {
            if(enableBuffer){
                sb.append(formatter.format(str.plus("\n"), level))
                if(sb.length > bufferSize){
                    BufferedOutputStream(FileOutputStream(File(filename), true)).use { bos ->
                        bos.write(sb.toString().encodeToByteArray())
                        sb.clear()
                    }
                }
            }else{
                BufferedOutputStream(FileOutputStream(File(filename), true)).use { bos ->
                    bos.write(formatter.format(str.plus("\n"), level).encodeToByteArray())
                }
            }

            if(rotation?.isRotateNeeded(filename) == true){
                rotation?.doRotate(filename)
            }
        }
    }

    fun flush(){
        BufferedOutputStream(FileOutputStream(File(filename), true)).use { bos ->
            bos.write(sb.toString().encodeToByteArray())
            sb.clear()
        }

        if(rotation?.isRotateNeeded(filename) == true){
            rotation?.doRotate(filename)
        }
    }
}