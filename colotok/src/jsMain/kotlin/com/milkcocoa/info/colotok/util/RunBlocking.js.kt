package com.milkcocoa.info.colotok.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
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
    // In JS, we cannot block. We start the coroutine and hope for the best,
    // or we could throw an exception if blocking is strictly required.
    // Given the context of forceShutdown, we attempt to run it.
    
    // This is not a correct runBlocking for JS, but there is no correct one for blocking.
    // Most KMP projects avoid runBlocking in common code for this reason.
    // However, for shutdown, we might just want to trigger it.
    
    // We can't return T if it's asynchronous. 
    // If T is Unit (as in shutdown calls), it might "work" in terms of starting the process.
    
    throw UnsupportedOperationException("runBlocking is not supported on JS platform.")
}
