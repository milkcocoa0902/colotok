package com.milkcocoa.info.colotok.core.formatter.details

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.core.logger.MDC
import com.milkcocoa.info.colotok.util.ThreadWrapper
import com.milkcocoa.info.colotok.util.color.AnsiColor
import com.milkcocoa.info.colotok.util.color.Color
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * base class for log formatter which used for structure log
 *
 * @constructor
 * @param field[List] log fields
 * @param mask[List] field name list to mask value with '*'
 */
abstract class StructuredFormatter(
    private val field: List<Element>,
    private val mask: List<String> = emptyList()
) : Formatter {
    private val lowerMask by lazy { mask.map { it.lowercase() } }

    override fun format(
        msg: String,
        level: Level
    ): String {
        return format(msg, level, mapOf())
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T : LogStructure> format(
        msg: T,
        serializer: KSerializer<T>,
        level: Level
    ): String {
        return format(msg, serializer, level, mapOf())
    }

    override fun format(
        msg: String,
        level: Level,
        attrs: Map<String, String>
    ): String {
        val mdc = MDC.getThreadLocalContext().data
        val dt = Clock.System.now()
        return buildJsonObject {
            field.forEach { f ->
                when (f) {
                    is Element.MESSAGE -> put("message", JsonPrimitive(msg))
                    is Element.LEVEL -> put("level", JsonPrimitive(level.name))
                    is Element.THREAD -> put("thread", JsonPrimitive(ThreadWrapper.getCurrentThreadName()))
                    is Element.ATTR -> attrs.forEach { (t, u) -> put(t, JsonPrimitive(u)) }
                    is Element.CUSTOM -> put(f.raw, JsonPrimitive(mdc.get(f.raw)?.toString() ?: ""))
                    is Element.CALLER -> put("caller", JsonPrimitive(ThreadWrapper.traceCallPoint()))
                    else -> {}
                }
            }

            currentTimeJsonField?.let {
                put("date", it)
            }
        }.toString()
    }

    private val currentTimeJsonField: JsonPrimitive? get() {
        val instant = Clock.System.now()
        val d = this.field.find { it == Element.DATE }
        val t = this.field.find { it == Element.TIME }
        val dt =
            this.field.find { it == Element.DATETIME } ?: run {
                if (d != null && t != null) {
                    return@run Element.DATETIME
                }
                return@run null
            }

        return dt?.let checkFormat@{
            if (d != null) {
                println(Color.foreground("[StructuredFormatter]: ${Element.DATE} is ignored.", AnsiColor.YELLOW))
            }
            if (t != null) {
                println(Color.foreground("[StructuredFormatter]: ${Element.TIME} is ignored.", AnsiColor.YELLOW))
            }

            return@checkFormat JsonPrimitive(LocalDateTime.Formats.ISO.format(instant.toLocalDateTime(TimeZone.UTC)))
        } ?: kotlin.run checkFormat@{
            d?.let {
                @Suppress("ktlint:standard:max-line-length")
                return@checkFormat JsonPrimitive(LocalDate.Formats.ISO.format(instant.toLocalDateTime(TimeZone.UTC).date))
            }
            t?.let {
                @Suppress("ktlint:standard:max-line-length")
                return@checkFormat JsonPrimitive(LocalTime.Formats.ISO.format(instant.toLocalDateTime(TimeZone.UTC).time))
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T : LogStructure> format(
        msg: T,
        serializer: KSerializer<T>,
        level: Level,
        attrs: Map<String, String>
    ): String {
        val s =
            object : JsonTransformingSerializer<T>(serializer) {
                override fun transformSerialize(element: JsonElement): JsonElement =
                    JsonObject(
                        element.jsonObject.mapValues {
                            if (this@StructuredFormatter.lowerMask.contains(it.key.lowercase())) {
                                return@mapValues JsonPrimitive(
                                    "*".repeat(it.value.toString().length.coerceAtMost(32))
                                )
                            }
                            return@mapValues when (it.value) {
                                is JsonArray -> JsonArray(it.value.jsonArray.map { transformSerialize(it) })
                                is JsonObject -> transformSerialize(it.value)
                                else -> it.value
                            }
                        }
                    )
            }

        val mdc = MDC.getThreadLocalContext().data
        return buildJsonObject {
            field.forEach { f ->
                when (f) {
                    is Element.MESSAGE -> put("message", Json.encodeToJsonElement(s, msg))
                    is Element.LEVEL -> put("level", JsonPrimitive(level.name))
                    is Element.THREAD -> put("thread", JsonPrimitive(ThreadWrapper.getCurrentThreadName()))
                    is Element.ATTR -> attrs.forEach { (t, u) -> put(t, JsonPrimitive(u)) }
                    is Element.CUSTOM -> put(f.raw, JsonPrimitive(mdc.get(f.raw)?.toString() ?: ""))
                    is Element.CALLER -> put("caller", JsonPrimitive(ThreadWrapper.traceCallPoint()))
                    else -> {}
                }
            }

            currentTimeJsonField?.let {
                put("date", it)
            }
        }.toString()
    }
}

interface LogStructure