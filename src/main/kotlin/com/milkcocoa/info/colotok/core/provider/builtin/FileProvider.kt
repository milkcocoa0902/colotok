package com.milkcocoa.info.colotok.core.provider.builtin

import com.milkcocoa.info.colotok.core.logger.LogLevel
import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.core.provider.details.ProviderConfig
import com.milkcocoa.info.colotok.core.provider.rotation.Rotation
import kotlinx.serialization.KSerializer
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString

class FileProvider(private val outputFileName: Path, config: FileProviderConfig) : Provider {

    /**
     * initialize provider with default configuration
     */
    constructor(outputFileName: Path): this(outputFileName, FileProviderConfig())

    /**
     * initialize provider with specified configuration
     */
    constructor(outputFileName: Path, config: FileProviderConfig.() -> Unit): this(outputFileName, FileProviderConfig().apply(config))
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

        override var formatter: Formatter = DetailTextFormatter

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

    private val filePath: Path get(){
        if(outputFileName.isDirectory()){
            return Path.of(outputFileName.pathString, "application.log")
        }
        return outputFileName
    }

    override fun write(name: String, msg: String, level: LogLevel) {
        write(name, msg, level, mapOf())
    }

    override fun write(name: String, msg: String, level: LogLevel, attr: Map<String, String>) {
        if(level.isEnabledFor(logLevel).not()){
            return
        }
        runCatching {
            if(enableBuffer){
                sb.appendLine(formatter.format(msg, level, attr))
                if(sb.length > bufferSize){
                    BufferedOutputStream(FileOutputStream(filePath.toFile(), true)).use { bos ->
                        bos.write(sb.toString().encodeToByteArray())
                        sb.clear()
                    }
                }
            }else{
                BufferedOutputStream(FileOutputStream(filePath.toFile(), true)).use { bos ->
                    bos.write(formatter.format(msg.plus("\n"), level, attr).encodeToByteArray())
                }
            }

            if(rotation?.isRotateNeeded(outputFileName) == true){
                rotation.doRotate(outputFileName)
            }
        }
    }

    override fun<T: LogStructure> write(name: String, msg: T, serializer: KSerializer<T>, level: LogLevel) {
        write(name, msg, serializer, level, mapOf())
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
                    BufferedOutputStream(FileOutputStream(filePath.toFile(), true)).use { bos ->
                        bos.write(sb.toString().encodeToByteArray())
                        sb.clear()
                    }
                }
            }else{
                BufferedOutputStream(FileOutputStream(filePath.toFile(), true)).use { bos ->
                    bos.write(formatter.format(msg, serializer, level, attr).plus("\n").encodeToByteArray())
                }
            }

            if(rotation?.isRotateNeeded(outputFileName) == true){
                rotation.doRotate(outputFileName)
            }
        }
    }

    fun flush(){
        BufferedOutputStream(FileOutputStream(filePath.toFile(), true)).use { bos ->
            bos.write(sb.toString().encodeToByteArray())
            sb.clear()
        }

        if(rotation?.isRotateNeeded(outputFileName) == true){
            rotation.doRotate(outputFileName)
        }
    }
}