package com.milkcocoa.info.colotok.core.formatter.details

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.level.Level
import com.milkcocoa.info.colotok.util.color.AnsiColor
import com.milkcocoa.info.colotok.util.color.Color
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.properties.Properties
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * base class for log formatter which used for structure log
 *
 * @constructor
 * @param field[List] log fields
 */
abstract class StructuredFormatter(private val field: List<Element>): Formatter{
    override fun format(msg: String, level: Level): String {
        return format(msg, level, mapOf())
    }
    @OptIn(ExperimentalSerializationApi::class)
    override fun<T: LogStructure> format(msg: T, serializer: KSerializer<T>, level: Level): String{
        return format(msg, serializer, level, mapOf())
    }

    override fun format(msg: String, level: Level, attrs: Map<String, String>): String {
        val dt = ZonedDateTime.now(ZoneId.systemDefault())
        return mutableMapOf<String, String>().apply {
            field.forEach { f ->
                when(f){
                    Element.MESSAGE -> put("msg", msg)
                    Element.LEVEL -> put("level", level.name)
                    Element.THREAD -> put("thread", Thread.currentThread().name)
                    Element.ATTR -> putAll(attrs)
                    else ->{}
                }
            }
            datetimeFormatter?.let {
                put("date", dt.format(it))
            }
        }.let {
            Json { encodeDefaults = true }
            Json.encodeToString(it)
        }
    }

    private val datetimeFormatter by lazy {
        val d = field.find { it == Element.DATE }
        val t = field.find { it == Element.TIME }

        val dt = field.find { it == Element.DATE_TIME } ?: run {
            if(d != null && t != null){
                return@run Element.DATE_TIME
            }
            return@run null
        }


        dt?.let {
            if(d != null){
                println(Color.foreground("[StructuredFormatter]: ${Element.DATE.name} is ignored.", AnsiColor.YELLOW))
            }
            if(t != null){
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
        val dt = ZonedDateTime.now(ZoneId.systemDefault())
        return mutableMapOf<String, String>().apply {
            field.forEach { f ->
                when(f){
                    Element.MESSAGE -> Properties.encodeToStringMap(serializer, msg).also { this.putAll(it) }
                    Element.LEVEL -> put("level", level.name)
                    Element.THREAD -> put("thread", Thread.currentThread().name)
                    Element.ATTR -> putAll(attrs)
                    else ->{}
                }
            }

            datetimeFormatter?.let {
                put("date", dt.format(it))
            }
        }.let {
            Json { encodeDefaults = true }
            Json.encodeToString(it)
        }
    }
}

interface LogStructure
