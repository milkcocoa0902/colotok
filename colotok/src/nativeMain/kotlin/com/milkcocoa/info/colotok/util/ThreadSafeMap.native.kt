package com.milkcocoa.info.colotok.util

actual fun <K, V> createThreadSafeMap(): MutableMap<K, V> = mutableMapOf<K, V>()
