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

    actual fun clear() = contextData.data.clear()

    actual fun getThreadLocalContext(): MDCContextData = contextData

    actual fun setThreadLocalContext(data: MDCContextData) {
        contextData.data.clear()
        contextData.data.putAll(data.data)
    }
}