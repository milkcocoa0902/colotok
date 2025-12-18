package com.milkcocoa.info.colotok.core.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

actual fun blocking(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> Unit
) = runBlocking(context, block)