package com.milkcocoa.info.colotok.util

import okio.Buffer
import okio.Sink

object SinkUtil {
    fun Sink.write(byteArray: ByteArray) = write(Buffer().apply { write(source = byteArray) }, byteArray.size.toLong())
}