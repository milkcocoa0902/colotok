@file:JsModule("async_hooks")
@file:JsNonModule

package com.milkcocoa.info.colotok.core.logger

external class AsyncLocalStorage<T> {
    fun <R> run(
        store: T,
        callback: () -> R
    ): R

    fun getStore(): T?
}