package com.milkcocoa.info.colotok.core.logger

import kotlinx.coroutines.ThreadContextElement
import kotlin.concurrent.getOrSet
import kotlin.coroutines.CoroutineContext

data class MDCContextData(
    val data: MutableMap<String, String> = mutableMapOf()
)

class MDCContext(
    private val contextData: MDCContextData
): ThreadContextElement<MDCContextData>{
    companion object Key : CoroutineContext.Key<MDCContext>
    override val key: CoroutineContext.Key<*> get() = Key

    override fun updateThreadContext(context: CoroutineContext): MDCContextData {
        val oldContext = MDC.getThreadLocalContext()
        MDC.setThreadLocalContext(contextData)
        return oldContext
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: MDCContextData) {
        MDC.setThreadLocalContext(oldState)
    }
}


object MDC {
    private val threadLocalContext = ThreadLocal<MDCContextData>()

    fun put(key: String, value: String) {
        val mdc = threadLocalContext.getOrSet { MDCContextData() }
        mdc.data[key] = value
    }

    fun get(key: String): String? = threadLocalContext.get().data[key]

    fun remove(key: String) = threadLocalContext.getOrSet { MDCContextData() }.data.remove(key)

    fun clear() = threadLocalContext.remove()

    fun getThreadLocalContext() = threadLocalContext.get() ?: MDCContextData()

    fun setThreadLocalContext(data: MDCContextData) {
        threadLocalContext.set(data)
    }

    fun asCoroutineContext() = MDCContext(threadLocalContext.get() ?: MDCContextData())
}


