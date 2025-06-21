package com.milkcocoa.info.colotok.core.logger

import org.slf4j.ILoggerFactory

class ColotokLoggerFactory4J: ILoggerFactory {
    private val cache = mutableMapOf<String, ColotokLogger4J>()

    override fun getLogger(name: String): ColotokLogger4J {
        return cache.getOrPut(name) {
            ColotokLogger4J(name)
        }
    }
}