package com.milkcocoa.info.clk.core.provider.builtin

import com.milkcocoa.info.clk.core.LogLevel
import com.milkcocoa.info.clk.core.provider.details.Provider
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
class FileProvider(val filename: String) : Provider {
    override val colorize: Boolean
        get() = false

    override fun write(name: String, str: String, level: LogLevel) {
        runCatching {
            BufferedOutputStream(FileOutputStream(File(filename), true)).use { bos ->
                bos.write(formatter.format(str.plus("\n"), level).encodeToByteArray())
            }
        }
    }
}