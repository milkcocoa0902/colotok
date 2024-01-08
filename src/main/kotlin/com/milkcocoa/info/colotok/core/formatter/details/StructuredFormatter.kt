package com.milkcocoa.info.colotok.core.formatter.details

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.util.color.AnsiColor
import com.milkcocoa.info.colotok.util.color.Color
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
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.swing.UIManager.put

/**
 * base class for log formatter which used for structure log
 *
 * @constructor
 * @param field[List] log fields
 * @param mask[List] field name list to mask value with '*'
 */
abstract class StructuredFormatter(private val field: List<Element>, private val mask: List<String>) : Formatter {
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
        val dt = ZonedDateTime.now(ZoneId.systemDefault())
        return buildJsonObject {
            field.forEach { f ->
                when (f) {
                    Element.MESSAGE -> put("message", JsonPrimitive(msg))
                    Element.LEVEL -> put("level", JsonPrimitive(level.name))
                    Element.THREAD -> put("thread", JsonPrimitive(Thread.currentThread().name))
                    Element.ATTR -> attrs.forEach { (t, u) -> put(t, JsonPrimitive(u)) }
                    else -> {}
                }
            }

            datetimeFormatter?.let {
                put("date", JsonPrimitive(dt.format(it)))
            }
        }.toString()
    }

    private val datetimeFormatter by lazy {
        val d = field.find { it == Element.DATE }
        val t = field.find { it == Element.TIME }

        val dt =
            field.find { it == Element.DATE_TIME } ?: run {
                if (d != null && t != null) {
                    return@run Element.DATE_TIME
                }
                return@run null
            }

        dt?.let {
            if (d != null) {
                println(Color.foreground("[StructuredFormatter]: ${Element.DATE.name} is ignored.", AnsiColor.YELLOW))
            }
            if (t != null) {
                println(Color.foreground("[StructuredFormatter]: ${Element.TIME.name} is ignored.", AnsiColor.YELLOW))
            }

            return@lazy DateTimeFormatter.ISO_OFFSET_DATE_TIME
        } ?: kotlin.run {
            d?.let {
                return@lazy DateTimeFormatter.ISO_LOCAL_DATE
            }
            t?.let {
                return@lazy DateTimeFormatter.ISO_LOCAL_TIME
            }
        }

        return@lazy null
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

        val dt = ZonedDateTime.now(ZoneId.systemDefault())
        return buildJsonObject {
            field.forEach { f ->
                when (f) {
                    Element.MESSAGE -> put("message", Json.encodeToJsonElement(s, msg))
                    Element.LEVEL -> put("level", JsonPrimitive(level.name))
                    Element.THREAD -> put("thread", JsonPrimitive(Thread.currentThread().name))
                    Element.ATTR -> attrs.forEach { (t, u) -> put(t, JsonPrimitive(u)) }
                    else -> {}
                }
            }

            datetimeFormatter?.let {
                put("date", JsonPrimitive(dt.format(it)))
            }
        }.toString()
    }
}

interface LogStructure