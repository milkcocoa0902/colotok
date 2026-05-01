package com.milkcocoa.info.colotok.core.provider.details

import com.milkcocoa.info.colotok.core.formatter.details.Formatter
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.metrics.MetricsCollectorSpec

/**
 * provider's basic config
 */
interface ProviderConfig {
    /**
     * providers log level
     */
    var level: Level

    /**
     * formatter
     */
    var formatter: Formatter

    /**
     * metrics collector specification.
     * default is [MetricsCollectorSpec.Inherit].
     */
    var metricsSpec: MetricsCollectorSpec

    /**
     * whether to enable internal metrics logging.
     * if true, metrics will be logged as [com.milkcocoa.info.colotok.core.logger.LogRecord.Metrics] to this provider.
     */
    var enableInternalMetricsLogging: Boolean
}

