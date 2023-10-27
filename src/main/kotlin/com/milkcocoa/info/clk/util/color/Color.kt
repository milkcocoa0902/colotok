package com.milkcocoa.info.clk.util.color

object Color {
    internal const val ESCAPE = '\u001B'
    internal const val RESET = "$ESCAPE[0m"

    fun foreground(str: String, ansiColor: AnsiColor) = Color.color(str, ansiColor)

    private fun color(string: String, ansiColor: AnsiColor) = "${ansiColor.fg}$string$RESET"
}