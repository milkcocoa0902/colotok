@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.milkcocoa.info.colotok.core.logger

import kotlinx.coroutines.ThreadContextElement
import kotlin.concurrent.getOrSet
import kotlin.coroutines.CoroutineContext

class MDCContext(
    private val contextData: MDCContextData
) : ThreadContextElement<MDCContextData> {
    companion object Key : CoroutineContext.Key<MDCContext>

    override val key: CoroutineContext.Key<*> get() = Key

    override fun updateThreadContext(context: CoroutineContext): MDCContextData {
        val oldContext = MDC.getThreadLocalContext()
        MDC.setThreadLocalContext(contextData)
        return oldContext
    }

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: MDCContextData
    ) {
        MDC.setThreadLocalContext(oldState)
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