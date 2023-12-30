package com.milkcocoa.info.colotok.core.provider.builtin

import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.core.provider.details.ProviderConfig
import com.milkcocoa.info.colotok.core.provider.rotation.Rotation
import com.milkcocoa.info.colotok.util.unit.Size.KiB
import kotlinx.serialization.KSerializer
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString

/**
 * builtin provider which used to write the log into file.
 */
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
         * size of buffer in Byte. which is used for save i/o.
         */
        var bufferSize = 4.KiB()

        /**
         * use buffering. if enable this, provider does not write to file until buffered data exceed `bufferSize`. if false write data into file immediately.
         */
        var enableBuffer = true

        /**
         * if not null, rotate after write
         */
        var rotation: Rotation? = null

        override var level: Level = LogLevel.DEBUG

        override var formatter: Formatter = DetailTextFormatter
    }

    private val enableBuffer = config.enableBuffer
    private val bufferSize = config.bufferSize
    private val rotation = config.rotation
    private val logLevel = config.level
    private val formatter = config.formatter

    private val sb: StringBuilder = StringBuilder()

    /**
     * get file path where to write the log.
     * if [outputFileName] is the directory, provider creates "application.log" under [outputFileName].
     */
    private val filePath: Path get(){
        if(outputFileName.isDirectory()){
            return Path.of(outputFileName.pathString, "application.log")
        }
        return outputFileName
    }


    override fun write(name: String, msg: String, level: Level, attr: Map<String, String>) {
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


    override fun <T : LogStructure> write(
        name: String,
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
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

    /**
     * flush buffered data into file.
     */
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