@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.milkcocoa.info.colotok.core.logger

import kotlinx.coroutines.CopyableThreadContextElement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlin.concurrent.getOrSet
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class MDCContext(
    contextData: MDCContextData
) : CopyableThreadContextElement<MDCContextData> {
    private var contextData = contextData.deepCopy()

    companion object Key : CoroutineContext.Key<MDCContext>

    override val key: CoroutineContext.Key<MDCContext> get() = Key

    override fun updateThreadContext(context: CoroutineContext): MDCContextData {
        val oldContext = MDC.getThreadLocalContext().deepCopy()
        MDC.installThreadLocalContext(contextData)
        return oldContext
    }

    override fun restoreThreadContext(
        context: CoroutineContext,
        oldState: MDCContextData
    ) {
        contextData = MDC.getThreadLocalContext().deepCopy()
        MDC.installThreadLocalContext(oldState)
    }

    override fun copyForChild(): MDCContext = MDCContext(contextData)

    override fun mergeForChild(overwritingElement: CoroutineContext.Element): CoroutineContext = overwritingElement
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

    actual fun clear() {
        getThreadLocalContext().data.clear()
        getThreadLocalContext().dequeData.clear()
    }

    actual fun getThreadLocalContext(): MDCContextData = threadLocalContext.getOrSet { MDCContextData() }

    actual fun setThreadLocalContext(data: MDCContextData) {
        val replacement = data.deepCopy()
        val current = getThreadLocalContext()
        current.data.clear()
        current.data.putAll(replacement.data)
        current.dequeData.clear()
        current.dequeData.putAll(replacement.dequeData)
    }

    internal fun installThreadLocalContext(data: MDCContextData) = threadLocalContext.set(data)

    fun asCoroutineContext() = MDCContext(getThreadLocalContext())
}

suspend inline fun <R> withMdcScope(crossinline block: suspend () -> R): R =
    withContext(MDC.asCoroutineContext()) { block() }