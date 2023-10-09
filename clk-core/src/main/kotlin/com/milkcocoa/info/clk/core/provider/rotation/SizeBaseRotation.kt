package com.milkcocoa.info.clk.core.provider.rotation

import com.milkcocoa.info.clk.util.unit.Size.KiB
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class SizeBaseRotation(val size: Long = 4L.KiB()): Rotation {
    override fun isRotateNeeded(filename: String): Boolean {
        return Files.size(Path.of(filename)) > size
    }

    override fun doRotate(filename: String) {
        val path = Path.of(filename)
        if(path.isDirectory()) return



        val rotateIndex = Files.list(Path.of(filename).parent).filter { it.isDirectory().not() and it.name.startsWith(path.name) }.count()
        Files.move(Path.of(filename), Path.of("${filename}.${rotateIndex}"), StandardCopyOption.REPLACE_EXISTING)
    }
}