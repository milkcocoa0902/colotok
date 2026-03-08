package com.milkcocoa.info.colotok.core.provider.builtin.file

import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.util.SinkUtil.write
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Path

/**
 * builtin provider which used to write the log into file.
 */
class FileProvider(private val outputFileName: okio.Path, config: FileProviderConfig) : Provider(config) {
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

    private val rotation = config.rotation

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

    private val mutex = Mutex()
    override suspend fun onMessage(record: LogRecord) {
        mutex.withLock {
            runCatching {
                getFileSystem().appendingSink(
                    file = filePath,
                    mustExist = false
                ).write(record.format(config.formatter).plus("\n").encodeToByteArray())

                if(rotation?.isRotateNeeded(filePath) == true){
                    rotation.doRotate(filePath)
                }
            }
        }
    }

    /**
     * flush buffered data into file.
     */
    override suspend fun onFlush() {
        runCatching {
            if (rotation?.isRotateNeeded(filePath) == true) {
                rotation.doRotate(filePath)
            }
        }
    }
}