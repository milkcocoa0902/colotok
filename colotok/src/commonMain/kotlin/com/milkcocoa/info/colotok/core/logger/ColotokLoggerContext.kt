package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProvider
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider

/**
 * builder class for get logger instance
 */
class ColotokLoggerContext {
    class FrozenContextException : IllegalStateException("LoggerContext is already frozen")


    private val providers: MutableList<Provider> = mutableListOf()
    private val attrs = mutableMapOf<String, String>()
    private var frozen: Boolean = false

    fun freeze() { frozen = true }
    private fun ensureNotFrozen() {
        if (frozen) throw FrozenContextException()
    }


    /**
     * fork current logger context with shallow copy
     * @return [ColotokLoggerContext] forked context
     */
    fun shallowCopy() = ColotokLoggerContext().apply {
        this.providers.addAll(this@ColotokLoggerContext.providers)
        this.withAttrs(this@ColotokLoggerContext.attrs.toMap())
    }

    fun withAttrs(attrs: Map<String, String>) = apply {
        ensureNotFrozen()
        this.attrs.clear()
        this.attrs.putAll(attrs)
    }


    fun putAttrs(attr: Map<String, String>) =
        apply {
            ensureNotFrozen()
            this@ColotokLoggerContext.attrs.putAll(attr)
        }

    /**
     * add provider to current logger
     * @param provider[Provider] provider to print log
     * @return [ColotokLoggerContext] this instance
     */
    fun addProvider(provider: Provider) =
        apply {
            ensureNotFrozen()
            providers.add(provider)
        }

    /**
     * generate logger instance with default name
     * @return [ColotokLogger] logger
     */
    fun getLogger(): ColotokLogger {
        return ColotokLogger(name = "Default Logger") {
            this.defaultAttrs = this@ColotokLoggerContext.attrs
            this.providers = this@ColotokLoggerContext.providers
        }
    }

    /**
     * generate logger instance with specified name
     * @param name[String] logger name
     * @return [ColotokLogger] logger
     */
    fun getLogger(name: String): ColotokLogger {
        return ColotokLogger(name = name) {
            this.defaultAttrs = this@ColotokLoggerContext.attrs
            this.providers = this@ColotokLoggerContext.providers
        }
    }


    companion object {
        private var _default: ColotokLoggerContext = ColotokLoggerContext()
            .addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
                this.formatter = DetailTextFormatter
                this.level = LogLevel.DEBUG
            }))

        public val DEFAULT: ColotokLoggerContext get() = _default
        public fun setDefault(logger: ColotokLoggerContext) { _default = logger }

    }
}