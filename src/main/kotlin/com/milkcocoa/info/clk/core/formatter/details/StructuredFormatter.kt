package com.milkcocoa.info.clk.core.formatter.details

import com.milkcocoa.info.clk.core.formatter.Element
import com.milkcocoa.info.clk.core.logger.LogLevel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.encodeToMap
import kotlinx.serialization.properties.encodeToStringMap
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.reflect.KClass

abstract class StructuredFormatter(private val field: List<Element>): Formatter{
    override fun format(msg: String, level: LogLevel): String {
        TODO("Not yet implemented")
    }
    @OptIn(ExperimentalSerializationApi::class)
    override fun<T: LogStructure> format(msg: T, selializer: KSerializer<T>, level: LogLevel): String{

        val dt = LocalDateTime.now()
        return mutableMapOf<String, String>().apply {
            var fmt = ""
            field.forEach { f ->
                when(f){
                    Element.MESSAGE -> Properties.encodeToStringMap(selializer, msg).also { this.putAll(it) }
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
