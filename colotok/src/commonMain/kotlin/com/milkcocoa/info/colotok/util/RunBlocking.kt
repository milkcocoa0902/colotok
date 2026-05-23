package com.milkcocoa.info.colotok.util

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * Runs a new coroutine and blocks the current thread until its completion.
 * This is a platform-specific replacement for kotlinx.coroutines.runBlocking.
 */
expect fun <T> runBlocking(
    context: CoroutineContext = kotlin.coroutines.EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T
): T
