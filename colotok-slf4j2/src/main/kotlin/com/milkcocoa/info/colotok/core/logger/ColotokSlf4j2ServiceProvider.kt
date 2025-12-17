package com.milkcocoa.info.colotok.core.logger

import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider

class ColotokSlf4j2ServiceProvider: SLF4JServiceProvider {
    private val markerFactory by lazy { BasicMarkerFactory() }
    private val mdcAdapter by lazy { ColotokMDCAdapter() }
    private val loggerFactory by lazy {
        ColotokLoggerFactory4J2(
            markerFactory = markerFactory,
            mdcAdapter = mdcAdapter
        )
    }


    override fun getLoggerFactory(): ILoggerFactory = loggerFactory

    override fun getMarkerFactory(): IMarkerFactory = markerFactory

    override fun getMDCAdapter(): MDCAdapter = mdcAdapter

    override fun getRequestedApiVersion(): String = "2.0.0"

    override fun initialize() {
    }
}