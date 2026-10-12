package com.milkcocoa.info.colotok.core.provider.builtin.file

import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.util.SinkUtil.write
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Path
import okio.Sink

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
    private val mutex = Mutex()

    private var destination: Pair<Sink, Path>? = null

    override suspend fun onMessage(record: LogRecord) {
        mutex.withLock {
            try {
                val bytes = record.format(config.formatter).plus("\n").encodeToByteArray()
                val (sink, path) = destination ?: openDestination()

                sink.write(bytes)
                sink.flush()

                if (rotation?.isRotateNeeded(path) == true) {
                    closeDestination()
                    rotation.doRotate(path)
                }
            } catch (throwable: Throwable) {
                closeDestination(throwable)
                throw throwable
            }
        }
    }

    /**
     * Flush and release the current sink, then check rotation.
     * The next record opens the destination again.
     */
    override suspend fun onFlush() {
        mutex.withLock {
            val (sink, path) = destination ?: return@withLock
            try {
                sink.flush()
                closeDestination()
                if (getFileSystem().exists(path) && rotation?.isRotateNeeded(path) == true) {
                    rotation.doRotate(path)
                }
            } catch (throwable: Throwable) {
                closeDestination(throwable)
                throw throwable
            }
        }
    }

    override fun onClosed() {
        // Provider invokes this callback after worker processing has stopped.
        closeDestination()
    }

    private fun openDestination(): Pair<Sink, Path> {
        val path =
            if (getFileSystem().metadataOrNull(outputFileName)?.isDirectory == true) {
                outputFileName / "application.log"
            } else {
                outputFileName
            }
        return (getFileSystem().appendingSink(file = path, mustExist = false) to path)
            .also { destination = it }
    }

    private fun closeDestination(cause: Throwable? = null) {
        val current = destination ?: return
        destination = null
        try {
            current.first.close()
        } catch (throwable: Throwable) {
            if (cause == null) throw throwable
            if (throwable !== cause) cause.addSuppressed(throwable)
        }
    }
}
