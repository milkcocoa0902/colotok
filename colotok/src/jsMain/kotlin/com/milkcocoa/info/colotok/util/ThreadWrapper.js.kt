package com.milkcocoa.info.colotok.util

import kotlin.experimental.ExperimentalNativeApi

@JsModule("process")
@JsNonModule
@Suppress("ktlint:standard:class-naming")
external object process {
    val pid: Int
}

actual object ThreadWrapper {
    actual fun getCurrentThreadName() = "PID: ${process.pid}"

    // How do I get a caller???
    @OptIn(ExperimentalNativeApi::class)
    actual fun traceCallPoint() = ""
}