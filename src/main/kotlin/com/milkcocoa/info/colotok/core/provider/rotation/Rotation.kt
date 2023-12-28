package com.milkcocoa.info.colotok.core.provider.rotation

import java.nio.file.Path

interface Rotation {
    fun isRotateNeeded(filePath: Path): Boolean
    fun doRotate(filePath: Path)
}