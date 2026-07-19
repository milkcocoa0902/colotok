package com.milkcocoa.info.colotok.core.provider.rotation

import com.milkcocoa.info.colotok.core.provider.builtin.file.getFileSystem
import okio.Path
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

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
        val metadata = getFileSystem().metadataOrNull(filePath) ?: return false
        return isRotationNeeded(
            createdAtMillis = metadata.createdAtMillis,
            lastModifiedAtMillis = metadata.lastModifiedAtMillis,
            period = period,
            now = Clock.System.now(),
        )
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

internal fun isRotationNeeded(
    createdAtMillis: Long?,
    lastModifiedAtMillis: Long?,
    period: Duration,
    now: Instant,
): Boolean {
    val baseTimestampMillis = createdAtMillis ?: lastModifiedAtMillis ?: return false
    val baseTimestamp = Instant.fromEpochMilliseconds(baseTimestampMillis)
    return baseTimestamp.plus(period) <= now
}
