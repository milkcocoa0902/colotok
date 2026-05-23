package com.milkcocoa.info.colotok.util

import java.util.Collections

actual fun <K, V> createThreadSafeMap(): MutableMap<K, V> = Collections.synchronizedMap(mutableMapOf<K, V>())
