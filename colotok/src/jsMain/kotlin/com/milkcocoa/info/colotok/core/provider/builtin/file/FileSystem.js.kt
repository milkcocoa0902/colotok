package com.milkcocoa.info.colotok.core.provider.builtin.file

import okio.FileSystem
import okio.NodeJsFileSystem

actual fun getFileSystem(): FileSystem = NodeJsFileSystem