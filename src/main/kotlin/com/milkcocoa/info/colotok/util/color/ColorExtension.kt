package com.milkcocoa.info.colotok.util.color

object ColorExtension {
    fun String.black() = Color.foreground(this, AnsiColor.BLACK)
    fun String.red() = Color.foreground(this, AnsiColor.RED)
    fun String.green() = Color.foreground(this, AnsiColor.GREEN)
    fun String.yellow() = Color.foreground(this, AnsiColor.YELLOW)
    fun String.blue() = Color.foreground(this, AnsiColor.BLUE)
    fun String.magenta() = Color.foreground(this, AnsiColor.MAGENTA)
    fun String.cyan() = Color.foreground(this, AnsiColor.CYAN)
    fun String.white() = Color.foreground(this, AnsiColor.WHITE)
}