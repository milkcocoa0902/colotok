package com.milkcocoa.info.colotok.core.logger

actual object MDC {
    private val threadLocalContext = AsyncLocalStorage<MDCContextData>()

    actual fun put(
        key: String,
        value: String
    ) {
        val currentData = threadLocalContext.getStore() ?: MDCContextData()
        currentData.data[key] = value

        threadLocalContext.run(currentData) { }
    }

    actual fun get(key: String): String? = threadLocalContext.getStore()?.data?.get(key)

    actual fun remove(key: String) = threadLocalContext.getStore()?.data?.remove(key)

    actual fun clear() {
        threadLocalContext.run(MDCContextData()) { }
    }

    actual fun getThreadLocalContext(): MDCContextData {
        return threadLocalContext.getStore() ?: MDCContextData()
    }

    actual fun setThreadLocalContext(data: MDCContextData) {
        threadLocalContext.run(data) { }
    }

    fun <R> withContext(block: () -> R): R {
        val mdcData = MDCContextData()
        return threadLocalContext.run(mdcData) {
            block()
        }
    }
}

inline fun <R> withMdcScope(crossinline block: () -> R): R = MDC.withContext { block() }