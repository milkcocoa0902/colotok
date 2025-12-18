package com.milkcocoa.info.colotok.core.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * JavaScript implementation of the blocking function.
 * 
 * IMPORTANT: In Kotlin/JS, true synchronous execution with `runBlocking` is not supported.
 * This implementation uses GlobalScope.launch instead, which means:
 * - The function returns immediately without waiting for the coroutine to complete
 * - The execution is fully asynchronous
 * - No synchronization guarantees are provided
 *
 * Use this with the understanding that it doesn't provide the same blocking behavior
 * as on other platforms.
 */
@OptIn(DelicateCoroutinesApi::class)
actual fun blocking(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> Unit
) {
    GlobalScope.launch(context) {
        block()
    }
}
