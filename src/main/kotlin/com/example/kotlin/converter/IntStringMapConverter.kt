package com.example.kotlin.converter

import com.fasterxml.jackson.core.type.TypeReference
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class IntStringMapConverter : AttributeConverter<Map<Int, String>, String> {

    private val objectMapper = createKotlinObjectMapper()

    override fun convertToDatabaseColumn(p0: Map<Int, String>?): String {
        return objectMapper.writeValueAsString(p0)
    }

    override fun convertToEntityAttribute(p0: String): Map<Int, String> {
        return objectMapper.readValue(
            p0,
            object : TypeReference<Map<Int, String>>() {}
        )
    }
}
