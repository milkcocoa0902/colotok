package com.milkcocoa.info.colotok.util.std

import java.io.InputStream

class StdIn : InputStream() {
    private val sb: StringBuilder = StringBuilder()
    private val lf = System.lineSeparator()

    public fun readline(str: String) = sb.append(str).append(lf)

    override fun read(): Int {
        if (sb.length == 0) {
            return -1
        }
        val result = sb.elementAt(0).code
        sb.deleteCharAt(0)
        return result
    }
}