package com.example.kotlin.kotlin_advanced.generic

class Person(
    var name: String = "홍길동"
) {
    val isKim: Boolean
        get() = name.startsWith("김")

    val maskingName: String
        get() = name[0] + (1 until name.length).joinToString("") { "*" }
}