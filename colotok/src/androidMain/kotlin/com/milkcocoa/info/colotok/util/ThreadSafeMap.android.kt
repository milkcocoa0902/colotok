package com.milkcocoa.info.colotok.util

import java.util.concurrent.ConcurrentHashMap

actual fun <K, V> createThreadSafeMap(): MutableMap<K, V> = ConcurrentHashMap<K, V>()
