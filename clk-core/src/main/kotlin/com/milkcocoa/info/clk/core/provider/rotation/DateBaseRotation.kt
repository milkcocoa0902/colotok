package com.milkcocoa.info.clk.core.provider.rotation

import com.milkcocoa.info.clk.util.unit.Size.KiB
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class DateBaseRotation(val period: Duration = 7.days): Rotation {
    override fun isRotateNeeded(filename: String): Boolean {
        val fileCreationTimeInstant =  Files.readAttributes(Path.of(filename), BasicFileAttributes::class.java).creationTime()
            .toInstant()
            .atZone(ZoneId.systemDefault())
        val currentTime = ZonedDateTime.now(ZoneId.systemDefault())
        return fileCreationTimeInstant.plusSeconds(period.inWholeSeconds) > currentTime
    }

    override fun doRotate(filename: String) {
        val path = Path.of(filename)
        if(path.isDirectory()) return



        val rotateIndex = Files.list(Path.of(filename).parent).filter { it.isDirectory().not() and it.name.startsWith(path.name) }.count()
        Files.move(Path.of(filename), Path.of("${filename}.${rotateIndex}"), StandardCopyOption.REPLACE_EXISTING)
    }
}