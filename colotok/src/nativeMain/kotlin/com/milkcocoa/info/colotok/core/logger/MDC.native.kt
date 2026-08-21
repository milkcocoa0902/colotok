package com.milkcocoa.info.colotok.core.logger

import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
actual object MDC {
    private val contextData = MDCContextData()

    actual fun put(
        key: String,
        value: String
    ) {
        contextData.data.put(key, value)
    }

    actual fun get(key: String): String? = contextData.data[key]

    actual fun remove(key: String): String? = contextData.data.remove(key)

    actual fun clear() {
        contextData.data.clear()
        contextData.dequeData.clear()
    }

    actual fun getThreadLocalContext(): MDCContextData = contextData

    actual fun setThreadLocalContext(data: MDCContextData) {
        val replacement = data.deepCopy()
        contextData.data.clear()
        contextData.data.putAll(replacement.data)
        contextData.dequeData.clear()
        replacement.dequeData.forEach { (key, deque) ->
            contextData.dequeData[key] = ArrayDeque(deque)
        }
    }
}