package com.milkcocoa.info.colotok.core.logger

import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.Logger
import org.slf4j.spi.MDCAdapter

class ColotokLoggerFactory4J2(
    private val markerFactory: IMarkerFactory,
    private val mdcAdapter: MDCAdapter
): ILoggerFactory {
    private val cache = mutableMapOf<String, ColotokLogger4J2>()
    override fun getLogger(name: String): Logger {
        return cache.computeIfAbsent(name){
            ColotokLogger4J2(
                name,
                markerFactory,
                mdcAdapter
            )
        }
    }
}