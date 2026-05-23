package com.milkcocoa.info.colotok.core.logger

import com.milkcocoa.info.colotok.core.formatter.builtin.text.DetailTextFormatter
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.metrics.CompositeMetricsCollector
import com.milkcocoa.info.colotok.core.metrics.InternalLoggingMetricsCollector
import com.milkcocoa.info.colotok.core.metrics.MetricsCollector
import com.milkcocoa.info.colotok.core.metrics.MetricsCollectorSpec
import com.milkcocoa.info.colotok.core.metrics.NoOpMetricsCollector
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProvider
import com.milkcocoa.info.colotok.core.provider.builtin.console.ConsoleProviderConfig
import com.milkcocoa.info.colotok.core.provider.details.Provider
import com.milkcocoa.info.colotok.util.createThreadSafeMap

/**
 * builder class for get logger instance
 */
class ColotokLoggerContext {
    class FrozenContextException : IllegalStateException("LoggerContext is already frozen")


    private val providers: MutableList<Provider> = mutableListOf()
    private val attrs = mutableMapOf<String, String>()
    private var immutableProviders: List<Provider> = emptyList()
    private var immutableAttrs: Map<String, String> = emptyMap()
    private var metricsCollector: MetricsCollector = NoOpMetricsCollector
    private var frozen: Boolean = false
    private val activeLoggers: MutableMap<String, ColotokLogger> = createThreadSafeMap()

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
        this.immutableProviders = this.providers.toList()
        this.withAttrs(this@ColotokLoggerContext.attrs.toMap())
    }

    fun withAttrs(attrs: Map<String, String>) = apply {
        ensureNotFrozen()
        this.attrs.clear()
        this.attrs.putAll(attrs)
        this.immutableAttrs = this.attrs.toMap()
    }

    /**
     * set metrics collector to current logger context
     * @param collector[MetricsCollector] metrics collector
     * @return [ColotokLoggerContext] this instance
     */
    fun withMetrics(collector: MetricsCollector) = apply {
        ensureNotFrozen()
        this.metricsCollector = collector
    }


    fun putAttrs(attr: Map<String, String>) =
        apply {
            ensureNotFrozen()
            this@ColotokLoggerContext.attrs.putAll(attr)
            this@ColotokLoggerContext.immutableAttrs = this@ColotokLoggerContext.attrs.toMap()
        }

    /**
     * add provider to current logger
     * @param provider[Provider] provider to print log
     * @return [ColotokLoggerContext] this instance
     */
    fun addProvider(provider: Provider) =
        apply {
            ensureNotFrozen()

            val baseCollector = when (val spec = provider.config.metricsSpec) {
                is MetricsCollectorSpec.Explicit -> {
                    if (spec.inheritParent) {
                        CompositeMetricsCollector(listOf(this.metricsCollector, spec.collector))
                    } else {
                        spec.collector
                    }
                }
                is MetricsCollectorSpec.Inherit -> this.metricsCollector
                is MetricsCollectorSpec.NoOp -> NoOpMetricsCollector
            }

            if (provider.config.enableInternalMetricsLogging) {
                val internalCollector = InternalLoggingMetricsCollector(provider)
                if (baseCollector == NoOpMetricsCollector) {
                    provider.effectiveMetricsCollector = internalCollector
                } else {
                    provider.effectiveMetricsCollector = CompositeMetricsCollector(listOf(baseCollector, internalCollector))
                }
            } else {
                provider.effectiveMetricsCollector = baseCollector
            }

            providers.add(provider)
            immutableProviders = providers.toList()
        }

    /**
     * generate logger instance with default name
     * @return [ColotokLogger] logger
     */
    fun getLogger(): ColotokLogger {
        return getLogger("Default Logger")
    }

    /**
     * generate logger instance with specified name
     * @param name[String] logger name
     * @return [ColotokLogger] logger
     */
    fun getLogger(name: String): ColotokLogger {
        return activeLoggers.getOrPut(name) {
            ColotokLogger(
                name = name,
                providersProvider = { this.immutableProviders },
                attrsProvider = { this.immutableAttrs }
            )
        }
    }

    /**
     * Shutdown all loggers created in this context and wait for all providers to finish processing.
     */
    suspend fun shutdown() {
        activeLoggers.values.forEach { it.shutdown() }
        activeLoggers.clear()
        providers.forEach { it.join() }
    }

    /**
     * Shutdown all loggers created in this context immediately.
     */
    fun forceShutdown() {
        activeLoggers.values.forEach { it.forceShutdown() }
        activeLoggers.clear()
    }


    companion object {
        /**
         * get default logger context
         */
        val default: ColotokLoggerContext get() = _default

        private var _default: ColotokLoggerContext = ColotokLoggerContext()
            .addProvider(ConsoleProvider(ConsoleProviderConfig().apply {
                this.formatter = DetailTextFormatter
                this.level = LogLevel.DEBUG
            }))

        public val DEFAULT: ColotokLoggerContext get() = _default
        public fun setDefault(logger: ColotokLoggerContext) { _default = logger }

    }
}