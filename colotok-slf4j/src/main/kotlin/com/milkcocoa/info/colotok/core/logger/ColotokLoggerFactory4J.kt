package com.milkcocoa.info.colotok.core.logger

import org.slf4j.ILoggerFactory
import java.util.concurrent.ConcurrentHashMap

class ColotokLoggerFactory4J : ILoggerFactory {
    private val cache = ConcurrentHashMap<String, ColotokLogger4J>()

    override fun getLogger(name: String): ColotokLogger4J {
        return cache.computeIfAbsent(name) {
            ColotokLogger4J(name)
        }
    }
}