package com.milkcocoa.info.colotok.core.provider.details

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun notifyShutdown(
    callbackScope: CoroutineScope,
    result: Result<Unit>,
    onComplete: suspend (Result<Unit>) -> Unit
) {
    // Dispatch before entering caller code: Unconfined or immediate dispatchers
    // must not execute user callbacks inside a provider completion handler.
    val callbackContext = callbackScope.coroutineContext.minusKey(Job)
    callbackScope.launch(Dispatchers.Default) {
        withContext(callbackContext) {
            onComplete(result)
        }
    }
}