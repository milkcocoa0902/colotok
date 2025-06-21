package org.slf4j.impl

import com.milkcocoa.info.colotok.core.logger.ColotokLoggerFactory4J
import org.slf4j.ILoggerFactory
import org.slf4j.spi.LoggerFactoryBinder

object StaticLoggerBinder : LoggerFactoryBinder {

    private val factory = ColotokLoggerFactory4J()

    override fun getLoggerFactory(): ILoggerFactory = factory

    override fun getLoggerFactoryClassStr(): String =
        ColotokLoggerFactory4J::class.java.name

    @JvmField
    val REQUESTED_API_VERSION: String = "1.7" // ← SLF4Jのバージョンと一致させる

    @JvmStatic
    fun getSingleton(): StaticLoggerBinder = this
}
