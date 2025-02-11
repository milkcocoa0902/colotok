package com.milkcocoa.info.colotok.core.logger

data class MDCContextData(
    val data: MutableMap<String, String> = mutableMapOf()
)

expect object MDC {
    fun put(
        key: String,
        value: String
    )

    fun get(key: String): String?

    fun remove(key: String): String?

    fun clear()

    fun getThreadLocalContext(): MDCContextData

    fun setThreadLocalContext(data: MDCContextData)
}