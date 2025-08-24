package com.example.kotlin.converter

import com.fasterxml.jackson.core.type.TypeReference
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringIntMapConverter : AttributeConverter<Map<String, Int>, String> {

    private val objectMapper = createKotlinObjectMapper()

    override fun convertToDatabaseColumn(p0: Map<String, Int>): String {
        return objectMapper.writeValueAsString(p0)
    }

    override fun convertToEntityAttribute(p0: String): Map<String, Int> {
        return objectMapper.readValue(p0, object : TypeReference<Map<String, Int>>() {}
        )
    }
}
