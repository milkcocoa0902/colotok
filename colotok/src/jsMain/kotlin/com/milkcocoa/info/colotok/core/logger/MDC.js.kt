package com.milkcocoa.info.colotok.core.logger

actual object MDC {
    private val threadLocalContext = AsyncLocalStorage<MDCContextData>()

    actual fun put(
        key: String,
        value: String
    ) {
        val currentData = threadLocalContext.getStore()?.deepCopy() ?: MDCContextData()
        currentData.data[key] = value
        threadLocalContext.enterWith(currentData)
    }

    actual fun get(key: String): String? = threadLocalContext.getStore()?.data?.get(key)

    actual fun remove(key: String): String? {
        val currentData = threadLocalContext.getStore()?.deepCopy() ?: return null
        val removed = currentData.data.remove(key)
        threadLocalContext.enterWith(currentData)
        return removed
    }

    actual fun clear() {
        threadLocalContext.enterWith(MDCContextData())
    }

    actual fun getThreadLocalContext(): MDCContextData {
        return threadLocalContext.getStore() ?: MDCContextData()
    }

    actual fun setThreadLocalContext(data: MDCContextData) {
        threadLocalContext.enterWith(data.deepCopy())
    }

    fun <R> withContext(block: () -> R): R {
        val mdcData = getThreadLocalContext().deepCopy()
        return threadLocalContext.run(mdcData) {
            block()
        }
    }
}

inline fun <R> withMdcScope(crossinline block: () -> R): R = MDC.withContext { block() }