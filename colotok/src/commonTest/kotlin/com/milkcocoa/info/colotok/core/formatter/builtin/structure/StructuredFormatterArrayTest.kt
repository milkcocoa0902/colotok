package com.milkcocoa.info.colotok.core.formatter.builtin.structure

import com.milkcocoa.info.colotok.core.formatter.Element
import com.milkcocoa.info.colotok.core.formatter.details.LogStructure
import com.milkcocoa.info.colotok.core.formatter.details.StructuredFormatter
import com.milkcocoa.info.colotok.core.level.LogLevel
import com.milkcocoa.info.colotok.core.logger.LogRecord
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class StructuredFormatterArrayTest {
    @Serializable
    private data class ArrayMessage(
        val strings: List<String>,
        val numbers: List<Int>,
        val flags: List<Boolean>,
        val nullable: List<String?>,
        val matrix: List<List<Int>>,
        val empty: List<String>
    ) : LogStructure

    @Serializable(with = RawMessageSerializer::class)
    private data class RawMessage(val content: JsonElement) : LogStructure

    private object RawMessageSerializer : KSerializer<RawMessage> {
        override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

        override fun serialize(encoder: Encoder, value: RawMessage) {
            encoder.encodeSerializableValue(JsonElement.serializer(), value.content)
        }

        override fun deserialize(decoder: Decoder): RawMessage =
            RawMessage(decoder.decodeSerializableValue(JsonElement.serializer()))
    }

    @Test
    fun builtin_formatters_preserve_primitive_nullable_nested_and_empty_arrays() {
        val message = ArrayMessage(
            strings = listOf("first", "second"),
            numbers = listOf(1, 2),
            flags = listOf(true, false),
            nullable = listOf(null, "value"),
            matrix = listOf(listOf(1), emptyList(), listOf(2, 3)),
            empty = emptyList()
        )
        val expected = Json.encodeToJsonElement(ArrayMessage.serializer(), message)
        val record = LogRecord.StructuredText(
            name = "arrays",
            msg = message,
            level = LogLevel.INFO,
            attr = emptyMap(),
            serializer = ArrayMessage.serializer()
        )

        listOf(SimpleStructureFormatter, DetailStructureFormatter).forEach { formatter ->
            assertEquals(expected, Json.parseToJsonElement(formatter.format(record)).jsonObject["message"])
        }
    }

    @Test
    fun masking_traverses_mixed_arrays_and_masks_matching_fields_without_changing_other_values() {
        val input = Json.parseToJsonElement(
            """
            {
                "items":["plain",1,true,null,[{"PASSWORD":"abc","visible":[2,null]}],{"secret":"xyz"}],
                "password":[1,2]
            }
            """.trimIndent()
        )
        val expected = Json.parseToJsonElement(
            """
            {
                "items":["plain",1,true,null,[{"PASSWORD":"*****","visible":[2,null]}],{"secret":"*****"}],
                "password":"*****"
            }
            """.trimIndent()
        )

        listOf(SimpleStructureFormatter, DetailStructureFormatter).forEach { formatter ->
            assertEquals(expected, formatRaw(formatter, input))
        }
        val unmasked = object : StructuredFormatter(listOf(Element.MESSAGE)) {}
        assertEquals(input, formatRaw(unmasked, input))
    }

    @Test
    fun custom_serializers_can_emit_array_primitive_and_null_roots() {
        listOf("[1,null,[true],{\"secret\":\"abc\"}]", "\"text\"", "42", "true", "null").forEach { source ->
            val input = Json.parseToJsonElement(source)
            val unmasked = object : StructuredFormatter(listOf(Element.MESSAGE)) {}
            assertEquals(input, formatRaw(unmasked, input))
        }
        val input = Json.parseToJsonElement("""[{"secret":"abc"}]""")
        val expected = Json.parseToJsonElement("""[{"secret":"*****"}]""")
        assertEquals(expected, formatRaw(SimpleStructureFormatter, input))
    }

    private fun formatRaw(formatter: StructuredFormatter, content: JsonElement): JsonElement? {
        val record = LogRecord.StructuredText(
            name = "raw",
            msg = RawMessage(content),
            level = LogLevel.INFO,
            attr = emptyMap(),
            serializer = RawMessageSerializer
        )
        return Json.parseToJsonElement(formatter.format(record)).jsonObject["message"]
    }
}