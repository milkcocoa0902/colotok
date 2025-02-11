package com.milkcocoa.info.colotok.core.provider.rotation

import com.milkcocoa.info.colotok.core.provider.builtin.file.getFileSystem
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okio.Path
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
            getFileSystem().metadata(filePath).createdAtMillis?.let {
                Instant.fromEpochMilliseconds(it)
            }!!
        val currentTime = Clock.System.now()
        return fileCreationTimeInstant.plus(period) > currentTime
    }

    override fun doRotate(filePath: Path) {
        val rotateIndex =
            getFileSystem().list(filePath.parent!!).also { println(it) }
                .filter { it.name.startsWith(filePath.name) }.also { println(it) }
                .mapNotNull { it.name.removePrefix("${filePath.name}.").toIntOrNull() }.also { println(it) }
                .maxOrNull()?.plus(1) ?: 1

        getFileSystem().atomicMove(
            filePath,
            filePath.parent!!.resolve("${filePath.name}.$rotateIndex")
        )
    }
}