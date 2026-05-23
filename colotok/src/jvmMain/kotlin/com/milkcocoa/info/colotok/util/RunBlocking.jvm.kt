package com.milkcocoa.info.colotok.util

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.runBlocking as kotlinxRunBlocking

actual fun <T> runBlocking(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> T
): T {
    return kotlinxRunBlocking(context, block)
}
