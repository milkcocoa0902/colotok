package com.milkcocoa.info.colotok.core.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * Executes a suspending function in a blocking way.
 * 
 * This function has platform-specific implementations:
 * - On JVM, Android, and Native platforms: Uses `runBlocking` to provide true synchronous behavior
 * - On JS platform: Cannot use `runBlocking` (not supported), so it launches a coroutine without blocking
 *
 * @param context The CoroutineContext to use for the coroutine
 * @param block The suspending function to execute
 */
expect fun blocking(
    context: CoroutineContext = Dispatchers.Default,
    block: suspend CoroutineScope.() -> Unit
)
