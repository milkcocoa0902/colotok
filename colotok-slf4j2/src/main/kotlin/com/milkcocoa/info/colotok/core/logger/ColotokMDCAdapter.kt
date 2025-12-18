package com.milkcocoa.info.colotok.core.logger

import org.slf4j.spi.MDCAdapter
import java.util.Deque

class ColotokMDCAdapter: MDCAdapter {
    override fun put(key: String?, `val`: String?) {
        key ?: return
        `val` ?: return

        MDC.put(key = key, value = `val`)
    }

    override fun get(key: String?): String? {
        key ?: return null
        return MDC.get(key = key)
    }

    override fun remove(key: String?) {
        key ?: return
        MDC.remove(key = key)
    }

    override fun clear() {
        MDC.clear()
    }

    override fun getCopyOfContextMap(): Map<String?, String?> {
        // 不変のコピーを返す（外部で破壊できない）
        return MDC.getThreadLocalContext().data.toMap()
    }

    override fun setContextMap(contextMap: Map<String?, String?>?) {
        val data = MDCContextData()
        if (contextMap != null) {
            // null キー/値を除外し、String/String に絞る
            val filtered: MutableMap<String, String> = mutableMapOf()
            for ((k, v) in contextMap) {
                if (k != null && v != null) filtered[k] = v
            }
            data.data.putAll(filtered)
        }
        MDC.setThreadLocalContext(data)
    }

    override fun pushByKey(key: String?, value: String?) {
        if (key == null || value == null) return

        val ctx = MDC.getThreadLocalContext()
        val deque = ctx.dequeData.getOrPut(key) { ArrayDeque() }
        deque.addFirst(value)

        // top を単一値として反映
        MDC.put(key, value)
    }

    override fun popByKey(key: String?): String? {
        if (key == null) return null

        val ctx = MDC.getThreadLocalContext()
        val deque = ctx.dequeData[key] ?: return null
        val popped = deque.removeFirstOrNull()

        val newTop = deque.firstOrNull()
        if (newTop == null) {
            // 空になったら掃除
            ctx.dequeData.remove(key)
            MDC.remove(key)
        } else {
            MDC.put(key, newTop)
        }

        return popped
    }

    override fun getCopyOfDequeByKey(key: String?): Deque<String?>? {
        if (key == null) return null

        val ctx = MDC.getThreadLocalContext()
        val deque = ctx.dequeData[key] ?: return null

        // 参照を返すと外部に破壊されるのでコピーを返す
        val copy = java.util.ArrayDeque<String?>()
        for (v in deque) {
            copy.addLast(v)
        }
        return copy
    }

    override fun clearDequeByKey(key: String?) {
        if (key == null) return

        val ctx = MDC.getThreadLocalContext()
        ctx.dequeData.remove(key)
        MDC.remove(key)
    }
}