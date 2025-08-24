package com.example.kotlin.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

fun createKotlinObjectMapper(): ObjectMapper {
    return ObjectMapper().apply { registerModule(KotlinModule.Builder().build()) }
}