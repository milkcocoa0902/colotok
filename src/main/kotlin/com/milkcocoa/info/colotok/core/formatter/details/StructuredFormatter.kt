package com.milkcocoa.info.colotok.core.formatter.details

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.logger.LogLevel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.properties.Properties
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * base class for log formatter which used for structure log
 *
 * @constructor
 * @param field[List] log fields
 */
abstract class StructuredFormatter(private val field: List<Element>): Formatter{
    override fun format(msg: String, level: LogLevel): String {

        val dt = LocalDateTime.now()
        return mutableMapOf<String, String>().apply {
            var fmt = ""
            field.forEach { f ->
                when(f){
                    Element.MESSAGE -> put("msg", msg)
                    Element.LEVEL -> put("level", level.name)
                    Element.THREAD -> put("thread", Thread.currentThread().name)
                    Element.DATE -> fmt = fmt.plus("yyyy-MM-dd ")
                    Element.TIME -> fmt = fmt.plus("HH:MM:ss ")
                    else ->{}
                }
            }
            if(fmt.isNotBlank()){
                put("date", dt.format(DateTimeFormatter.ofPattern(fmt.trim())))
            }
        }.let {
            Json { encodeDefaults = true }
            Json.encodeToString(it)
        }
    }
    @OptIn(ExperimentalSerializationApi::class)
    override fun<T: LogStructure> format(msg: T, serializer: KSerializer<T>, level: LogLevel): String{

        val dt = LocalDateTime.now()
        return mutableMapOf<String, String>().apply {
            var fmt = ""
            field.forEach { f ->
                when(f){
                    Element.MESSAGE -> Properties.encodeToStringMap(serializer, msg).also { this.putAll(it) }
                    Element.LEVEL -> put("level", level.name)
                    Element.THREAD -> put("thread", Thread.currentThread().name)
                    Element.DATE -> fmt = fmt.plus("yyyy-MM-dd ")
                    Element.TIME -> fmt = fmt.plus("HH:MM:ss ")
                    else ->{}
                }
            }
            if(fmt.isNotBlank()){
                put("date", dt.format(DateTimeFormatter.ofPattern(fmt.trim())))
            }
        }.let {
            Json { encodeDefaults = true }
            Json.encodeToString(it)
        }
    }
}

interface LogStructure
