package quality.benchmarks.platforms

import com.milkcocoa.info.colotok.core.formatter.builtin.text.PlainTextFormatter
import com.milkcocoa.info.colotok.core.formatter.builtin.text.SimpleTextFormatter
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import com.milkcocoa.info.colotok.core.logger.MDC
import com.milkcocoa.info.colotok.core.logger.eventCallerSnapshot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.time.Instant
import kotlin.time.TimeSource

expect fun platformName(): String
expect fun consume(value: Any?)
expect fun allocatedBytes(): Long
expect fun platformPrint(message: String)
expect fun publish(message: String)
expect fun runtimeArguments(arguments: Array<String>): Array<String>

private fun candidateSimple(record: LogRecord.PlainText): String {
    val dt = record.eventTimestamp.toLocalDateTime(TimeZone.UTC)
    return buildString {
        append(dt.date.format(LocalDate.Formats.ISO))
        append(' ')
        append(dt.time.format(LocalTime.Formats.ISO))
        append("  [")
        append(record.level)
        append("] - ")
        append(record.msg)
    }
}

private fun measure(windowMs: Long, units: Int, operation: () -> Any?): Triple<Double, Double, Long> {
    val before = allocatedBytes()
    val start = TimeSource.Monotonic.markNow()
    var operations = 0L
    do {
        repeat(64) { consume(operation()) }
        operations += 64
    } while (start.elapsedNow().inWholeMilliseconds < windowMs)
    val nanos = start.elapsedNow().inWholeNanoseconds
    val after = allocatedBytes()
    val totalUnits = operations * units
    return Triple(nanos.toDouble() / totalUnits, if (before >= 0 && after >= before) (after - before).toDouble() / totalUnits else -1.0, totalUnits)
}

fun main(arguments: Array<String>) {
    val args = runtimeArguments(arguments)
    val variant = args.getOrElse(0) { "current" }
    val selected = args.getOrElse(1) { "all" }
    val windowMs = args.getOrElse(2) { "400" }.toLong()
    MDC.clear()
    val timestamp = Instant.parse("2020-02-03T04:05:06.789Z")
    val records = List(32) {
        LogRecord.PlainText("benchmark", "benchmark message $it " + "x".repeat(96), LogLevel.INFO, emptyMap(), timestamp)
    }
    records.forEach {
        check(candidateSimple(it) == it.format(SimpleTextFormatter))
        check(it.msg == it.format(PlainTextFormatter))
    }
    val caller = records.first().eventCallerSnapshot
    if (variant == "noCaller" || platformName() == "js") check(caller.isEmpty())
    publish(buildJsonObject {
        put("type", JsonPrimitive("environment"))
        put("platform", JsonPrimitive(platformName()))
        put("variant", JsonPrimitive(variant))
        put("callerExample", JsonPrimitive(caller))
        put("windowMs", JsonPrimitive(windowMs))
        put("warmupWindows", JsonPrimitive(3))
        put("measurementWindows", JsonPrimitive(5))
    }.toString())
    var index = 0
    fun next(): LogRecord.PlainText = records[(index++ and 31)]
    val lines = records.map { it.msg }
    val batches = List(32) { start -> (0 until 16).joinToString("\n") { lines[(start + it) and 31] } }
    val cases: List<Pair<String, () -> Any?>> = listOf(
        "consumeOnly" to { next().msg },
        "record" to { LogRecord.PlainText("benchmark", next().msg, LogLevel.INFO, emptyMap()) },
        "recordAndSimple" to { LogRecord.PlainText("benchmark", next().msg, LogLevel.INFO, emptyMap()).format(SimpleTextFormatter) },
        "formatPlain" to { next().format(PlainTextFormatter) },
        "formatPlainCandidate" to { next().msg },
        "formatSimple" to { next().format(SimpleTextFormatter) },
        "formatSimpleCandidate" to { candidateSimple(next()) },
        "printOne" to { platformPrint(lines[(index++ and 31)]); null },
        "printBatch16" to { platformPrint(batches[(index++ and 31)]); null }
    )
    for ((name, operation) in cases) {
        if (selected != "all" && selected != name) continue
        val units = if (name == "printBatch16") 16 else 1
        repeat(3) { measure(windowMs, units, operation) }
        repeat(5) { iteration ->
            val (ns, allocation, count) = measure(windowMs, units, operation)
            publish(buildJsonObject {
                put("type", JsonPrimitive("measurement"))
                put("platform", JsonPrimitive(platformName()))
                put("variant", JsonPrimitive(variant))
                put("case", JsonPrimitive(name))
                put("iteration", JsonPrimitive(iteration))
                put("nsPerUnit", JsonPrimitive(ns))
                put("allocatedBytesPerUnit", JsonPrimitive(allocation))
                put("units", JsonPrimitive(count))
            }.toString())
        }
    }
}
