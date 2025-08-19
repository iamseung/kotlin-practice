package com.example.kotlin.single_test

import org.junit.jupiter.api.Test

class single {

    @Test
    fun `test`() {
        // given
        val person1 = Person("John")
        val person2 = Person("John")
        person1.age = 10
        person2.age = 20

        person1 == person2 // true
        println(person1 == person2)

        // when

        // then
    }

    data class Person(val name: String) {
        var age: Int = 0
    }
}