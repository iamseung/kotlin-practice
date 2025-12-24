package com.example.kotlin.individual

import org.hibernate.internal.util.collections.CollectionHelper.listOf
import org.junit.jupiter.api.Test

class SingleTest {

    @Test
    fun `-연산을 테스트한다`() {
        // given
        val values = listOf("Context", "context")

        // when
        println(values - "Context" - "A")

        // then
    }

    @Test
    fun `test`() {
        // given
        val value = mapOf("SS" to Person(name = "SS", age = 1))

        // then
    }

    data class Person(val name: String, val age: Int)
}