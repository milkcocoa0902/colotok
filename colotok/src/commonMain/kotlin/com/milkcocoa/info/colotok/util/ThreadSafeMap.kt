package com.milkcocoa.info.colotok.util

/**
 * Creates a platform-specific thread-safe mutable map.
 */
expect fun <K, V> createThreadSafeMap(): MutableMap<K, V>
