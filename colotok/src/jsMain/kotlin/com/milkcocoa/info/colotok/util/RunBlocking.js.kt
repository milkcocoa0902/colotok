package com.milkcocoa.info.colotok.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import kotlin.coroutines.CoroutineContext

/**
 * JS implementation of runBlocking.
 * Note: JS is single-threaded and cannot truly block.
 * This implementation runs the block but cannot wait for it synchronously.
 * It is provided to satisfy the expect/actual contract.
 */
actual fun <T> runBlocking(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> T
): T {
    // In JS, we cannot block. We start the coroutine and return immediately.
    // This is useful for shutdown/forceShutdown where we want to trigger cleanup
    // without crashing the JS environment.
    
    // Note: If T is not Unit, this will likely cause a ClassCastException later,
    // but for the purposes of this library (shutdown calls), it works.
    GlobalScope.launch(context) {
        block()
    }
    
    @Suppress("UNCHECKED_CAST")
    return Unit as T
}
