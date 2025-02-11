package com.milkcocoa.info.colotok.core.provider.builtin.file

import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File

actual fun getFileSystem(): FileSystem = FileSystem.SYSTEM

fun File.asPath() = this.toOkioPath()