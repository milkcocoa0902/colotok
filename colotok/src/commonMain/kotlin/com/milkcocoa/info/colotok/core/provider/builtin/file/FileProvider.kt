package com.milkcocoa.info.colotok.core.provider.builtin.file

import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.util.SinkUtil.write
import okio.Path

/**
 * builtin provider which used to write the log into file.
 */
class FileProvider(private val outputFileName: okio.Path, config: FileProviderConfig) : Provider() {
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

    override fun onMessage(record: LogRecord) {
        if(record.level.isEnabledFor(logLevel).not()) return

        runCatching {
            if(enableBuffer){
                sb.appendLine(record.format(formatter))
                if(sb.length > bufferSize){
                    getFileSystem().appendingSink(
                        file = filePath,
                        mustExist = false
                    ).write(sb.toString().encodeToByteArray())
                    sb.clear()
                }
            }else{
                getFileSystem().appendingSink(
                    file = filePath,
                    mustExist = false
                ).write(record.format(formatter).plus("\n").encodeToByteArray())
            }

            if(rotation?.isRotateNeeded(filePath) == true){
                rotation.doRotate(filePath)
            }
        }
    }

    /**
     * flush buffered data into file.
     */
    override suspend fun onFlush() {
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