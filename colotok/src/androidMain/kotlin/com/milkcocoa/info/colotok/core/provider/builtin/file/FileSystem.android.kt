package com.milkcocoa.info.colotok.core.provider.builtin.file

import okio.FileSystem

actual fun getFileSystem(): FileSystem = FileSystem.SYSTEM