package com.example.kotlin.kotlin_advanced.reflection

import org.assertj.core.internal.Integers
import org.junit.jupiter.api.Test

class DITest {


    @Test
    fun kClass_Test() {
        val person = Person("이승석", 30)
        person
        val kClass = Person::class
        val constructors = kClass.constructors
        constructors.forEach { constructor -> println(constructor.parameters) }

        println("==================================================================================")
        println(kClass.qualifiedName)
        println(kClass.constructors)
        println(kClass.typeParameters)
    }

    data class Person(val name: String, val age: Int)

    // 제네릭 없이
    class NoteV1 {
        private val notes = mutableSetOf<Any>() // 아무거나 다 들어감

        fun add(item: Any) { notes.add(item) }
        fun first(): Any = notes.first()
    }

    /*
        T는 "이 클래스를 사용하는 쪽에서 타입을 결정해라" 라는 뜻

        사용할 때 타입이 결정됨
        val stringNote = Note<String>()    // T = String → Set<String>
        val intNote = Note<Int>()          // T = Int → Set<Int>
        val personNote = Note<Person>()    // T = Person → Set<Person>

        <T> = "타입을 비워두는 구멍"

        클래스 선언: Note<T>     ← 구멍을 뚫어둠
        클래스 사용: Note<String> ← 구멍을 String으로 채움
        채운 순간, 클래스 내부의 모든 T가 String으로 고정됨

        즉, <T>는 타입 안전성과 재사용성을 동시에 얻기 위한 장치입니다. 하나의 클래스로 여러 타입을 지원하면서도, 사용할 때는 타입이 섞이지 않도록 컴파일러가 검증해줍니다.
     */
    class NoteV2<T> {
        private val notes = mutableSetOf<T>()

        fun add(item: T) { notes.add(item) }
        fun first(): T = notes.first()
    }
}