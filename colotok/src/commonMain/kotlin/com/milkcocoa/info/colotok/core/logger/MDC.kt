package com.milkcocoa.info.colotok.core.logger

data class MDCContextData(
    val data: MutableMap<String, String> = mutableMapOf(),
    val dequeData: MutableMap<String, ArrayDeque<String>> = mutableMapOf()
){
    fun deepCopy(): MDCContextData {
        val copiedData = data.toMutableMap()

        val copiedDeque = mutableMapOf<String, ArrayDeque<String>>()
        for ((k, dq) in dequeData) {
            val dqCopy = ArrayDeque<String>()
            for (v in dq) {
                dqCopy.addLast(v)
            }
            copiedDeque[k] = dqCopy
        }

        return MDCContextData(
            data = copiedData,
            dequeData = copiedDeque
        )
    }

    fun copy(): MDCContextData = deepCopy()
}

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