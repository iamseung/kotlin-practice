package com.example.kotlin.kotlin_advanced.generic

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PersonTest {
    private val person = Person()

    @Test
    fun iskimTets() {
        // given
        val person = Person().apply { name = "김수한무" }

        // when & then
        assertThat(person.isKim).isTrue()
    }

    @Test
    fun maskingTest() {
        // given
        val person = Person().apply { name = "김무" }

        // when & then
        assertThat(person.maskingName).isEqualTo("김***")
    }
}