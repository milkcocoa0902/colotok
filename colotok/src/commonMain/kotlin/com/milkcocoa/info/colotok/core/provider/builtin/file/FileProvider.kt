package com.milkcocoa.info.colotok.core.provider.builtin.file

import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.util.SinkUtil.write
import kotlinx.serialization.KSerializer
import okio.Path

/**
 * builtin provider which used to write the log into file.
 */
class FileProvider(private val outputFileName: okio.Path, config: FileProviderConfig) : Provider {
    /**
     * initialize provider with default configuration
     */
    constructor(outputFileName: Path) : this(outputFileName, FileProviderConfig())

    /**
     * initialize provider with specified configuration
     */
    constructor(
        outputFileName: Path,
        config: FileProviderConfig.() -> Unit
    ) : this(outputFileName, FileProviderConfig().apply(config))

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
    private val filePath: Path
        get() {
            if (getFileSystem().exists(outputFileName)) {
                // output target exists
                if (getFileSystem().metadata(outputFileName).isDirectory) {
                    return outputFileName / "application.log"
                }
                return outputFileName
            } else {
                // output target does not exist
                // maybe output target is file
                return outputFileName
            }
        }

    override fun write(
        name: String,
        msg: String,
        level: Level,
        attr: Map<String, String>
    ) {
        if (level.isEnabledFor(logLevel).not()) {
            return
        }
        runCatching {
            if (enableBuffer) {
                sb.appendLine(formatter.format(msg, level, attr))
                if (sb.length > bufferSize) {
                    getFileSystem().appendingSink(
                        file = filePath,
                        mustExist = false
                    ).write(sb.toString().encodeToByteArray())
                    sb.clear()
                }
            } else {
                getFileSystem().appendingSink(
                    file = filePath,
                    mustExist = false
                ).write(formatter.format(msg.plus("\n"), level, attr).encodeToByteArray())
            }

            if (rotation?.isRotateNeeded(filePath) == true) {
                rotation.doRotate(filePath)
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
        if (level.isEnabledFor(logLevel).not()) {
            return
        }
        runCatching {
            if (enableBuffer) {
                sb.appendLine(formatter.format(msg, serializer, level, attr))
                if (sb.length > bufferSize) {
                    getFileSystem().appendingSink(
                        file = filePath,
                        mustExist = false
                    ).write(sb.toString().encodeToByteArray())
                    sb.clear()
                }
            } else {
                getFileSystem().appendingSink(
                    file = filePath,
                    mustExist = false
                ).write(formatter.format(msg, serializer, level, attr).plus("\n").encodeToByteArray())
            }

            if (rotation?.isRotateNeeded(filePath) == true) {
                rotation.doRotate(filePath)
            }
        }
    }

    /**
     * flush buffered data into file.
     */
    fun flush() {
        getFileSystem().appendingSink(
            file = filePath,
            mustExist = false
        ).write(sb.toString().encodeToByteArray())
        sb.clear()

        if (rotation?.isRotateNeeded(filePath) == true) {
            rotation.doRotate(filePath)
        }
    }
}