package com.milkcocoa.info.colotok.util

expect object ThreadWrapper {
    fun getCurrentThreadName(): String

    fun traceCallPoint(): String
}