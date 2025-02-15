package com.milkcocoa.info.colotok.core.provider.rotation

import okio.Path

/**
 * log rotation interface
 */
interface Rotation {
    /**
     * check for the log-rotation is needed.
     *
     * @param filePath[Path] current log file
     * @return [Boolean] true if log rotation is needed.
     */
    fun isRotateNeeded(filePath: Path): Boolean

    /**
     * do log-rotation.
     * @param filePath[Path] current log file
     */
    fun doRotate(filePath: Path)
}