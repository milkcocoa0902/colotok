package com.milkcocoa.info.clk.core.provider.rotation

import java.nio.file.Path

interface Rotation {
    fun isRotateNeeded(filePath: Path): Boolean
    fun doRotate(filePath: Path)
}