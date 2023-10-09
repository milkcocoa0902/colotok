package com.milkcocoa.info.clk.core.provider.rotation

interface Rotation {
    fun isRotateNeeded(filename: String): Boolean
    fun doRotate(filename: String)
}