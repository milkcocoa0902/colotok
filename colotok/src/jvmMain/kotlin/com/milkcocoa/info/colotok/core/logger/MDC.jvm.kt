@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.milkcocoa.info.colotok.core.logger

import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import kotlin.concurrent.getOrSet
import kotlin.coroutines.CoroutineContext

class MDCContext(
    private val contextData: MDCContextData
) : ThreadContextElement<MDCContextData> {
    companion object Key : CoroutineContext.Key<MDCContext>
    override val key: CoroutineContext.Key<MDCContext> get() = Key

    override fun updateThreadContext(context: CoroutineContext): MDCContextData {
        val oldContext = MDC.getThreadLocalContext().deepCopy()
        MDC.setThreadLocalContext(contextData.copy())
        return oldContext.copy()
    }

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: MDCContextData
    ) {
        MDC.setThreadLocalContext(oldState.copy())
    }
}

actual object MDC {
    private val threadLocalContext = ThreadLocal<MDCContextData>()

    actual fun put(
        key: String,
        value: String
    ) {
        val mdc = threadLocalContext.getOrSet { MDCContextData() }
        mdc.data[key] = value
    }

    actual fun get(key: String): String? = threadLocalContext.get()?.data[key]

    actual fun remove(key: String) = threadLocalContext.getOrSet { MDCContextData() }.data.remove(key)

    actual fun clear() = threadLocalContext.remove()

    actual fun getThreadLocalContext() = threadLocalContext.get() ?: MDCContextData()

    actual fun setThreadLocalContext(data: MDCContextData) {
        threadLocalContext.set(data)
    }

    fun asCoroutineContext() = MDCContext(threadLocalContext.get() ?: MDCContextData())
}

suspend inline fun <R> withMdcScope(
    crossinline block: suspend () -> R
): R = withContext(MDC.asCoroutineContext()) { block() }