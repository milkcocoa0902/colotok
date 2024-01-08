package com.milkcocoa.info.colotok.util.std

import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.StringReader

class StdOut : PrintStream(ByteArrayOutputStream()) {
    private var br = BufferedReader(StringReader(""))

    /**
     * 1行分の文字列を読み込む
     * @return 改行を含まない文字。終端の場合はnull
     */
    fun readLine(): String? {
        var line = ""
        br.readLine()?.let { return it }
        br = BufferedReader(StringReader(out.toString()))
        (out as ByteArrayOutputStream).reset()
        return br.readLine()
    }
}