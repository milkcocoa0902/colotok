package com.milkcocoa.info.colotok.core.provider.rotation

import com.milkcocoa.info.colotok.util.unit.Size.KiB
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.isDirectory
import kotlin.io.path.name

/**
 * size base log-rotation.
 * if log file exceeds the [size], log file will renew.
 *
 * application.log -> application.log.1 , application.log.2 , .... and create new application.log
 *
 * @constructor
 * @param size[Long] max log file size in Byte
 */
class SizeBaseRotation(val size: Long = 4L.KiB()): Rotation {
    override fun isRotateNeeded(filePath: Path): Boolean {
        return Files.size(filePath) > size
    }

    override fun doRotate(filePath: Path) {
        val rotateIndex = Files.list(filePath.parent).filter { it.isDirectory().not() and it.name.startsWith(filePath.name) }.count()
        Files.move(filePath, Path.of("${filePath}.${rotateIndex}"), StandardCopyOption.REPLACE_EXISTING)
    }
}