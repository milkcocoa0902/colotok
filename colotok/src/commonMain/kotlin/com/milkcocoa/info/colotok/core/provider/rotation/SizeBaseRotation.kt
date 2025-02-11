package com.milkcocoa.info.colotok.core.provider.rotation

import com.milkcocoa.info.colotok.core.provider.builtin.file.getFileSystem
import com.milkcocoa.info.colotok.util.unit.Size.KiB
import okio.Path

/**
 * size base log-rotation.
 * if log file exceeds the [size], log file will renew.
 *
 * application.log -> application.log.1 , application.log.2 , .... and create new application.log
 *
 * @constructor
 * @param size[Long] max log file size in Byte
 */
class SizeBaseRotation(val size: Long = 4L.KiB()) : Rotation {
    override fun isRotateNeeded(filePath: Path): Boolean {
        return (getFileSystem().metadata(filePath).size ?: 0) > size
    }

    override fun doRotate(filePath: Path) {
        val rotateIndex =
            getFileSystem().list(filePath.parent!!)
                .filter { it.name.startsWith(filePath.name) }
                .mapNotNull { it.name.removePrefix("${filePath.name}.").toIntOrNull() }
                .maxOrNull()?.plus(1) ?: 1

        getFileSystem().atomicMove(
            filePath,
            filePath.parent!!.resolve("${filePath.name}.$rotateIndex")
        )
    }
}