package com.milkcocoa.info.clk.core.builtin

import com.milkcocoa.info.clk.core.details.Provider
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class FileProvider(val filename: String): Provider  {
    override val colorize: Boolean
        get() = false

    override fun write(str: String) {
        runCatching {
            BufferedOutputStream(FileOutputStream(File(filename), true)).use { bos ->
                bos.write(str.plus("\n").encodeToByteArray())
            }
        }
    }

}