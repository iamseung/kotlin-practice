package com.example.kotlin.converter

import com.fasterxml.jackson.core.type.TypeReference
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringStringMapConverter : AttributeConverter<Map<String, String>, String> {

    private val objectMapper = createKotlinObjectMapper()

    override fun convertToDatabaseColumn(p0: Map<String, String>): String {
        return objectMapper.writeValueAsString(p0)
    }

    override fun convertToEntityAttribute(p0: String): Map<String, String> {
        return objectMapper.readValue(p0, object : TypeReference<Map<String, String>>() {})
    }
}
