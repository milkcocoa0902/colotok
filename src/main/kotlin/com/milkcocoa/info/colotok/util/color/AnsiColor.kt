package com.milkcocoa.info.colotok.util.color

enum class AnsiColor(code: Int) {
    BLACK(30),
    RED(31),
    GREEN(32),
    YELLOW(33),
    BLUE(34),
    MAGENTA(35),
    CYAN(36),
    WHITE(37);

    private val ESCAPE = '\u001B'
    private val RESET = "$ESCAPE[0m"

    val fg = "$ESCAPE[${code}m"
}