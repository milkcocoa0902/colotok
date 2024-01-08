package com.milkcocoa.info.colotok.core.provider.rotation

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * date base log-rotation.(duration base)
 * if log file passed [period], log file will renew
 *
 * application.log -> application.log.1 , application.log.2 , .... and create new application.log
 *
 * @constructor
 * @param period[Duration] log file's  life-time.
 */
class DateBaseRotation(private val period: Duration = 7.days) : Rotation {
    override fun isRotateNeeded(filePath: Path): Boolean {
        val fileCreationTimeInstant =
            Files.readAttributes(filePath, BasicFileAttributes::class.java).creationTime()
                .toInstant()
                .atZone(ZoneId.systemDefault())
        val currentTime = ZonedDateTime.now(ZoneId.systemDefault())
        return fileCreationTimeInstant.plusSeconds(period.inWholeSeconds) > currentTime
    }

    override fun doRotate(filePath: Path) {
        val rotateIndex =
            Files.list(filePath.parent).filter {
                it.isDirectory().not() and it.name.startsWith(filePath.name)
            }.count()
        Files.move(filePath, Path.of("$filePath.$rotateIndex"), StandardCopyOption.REPLACE_EXISTING)
    }
}