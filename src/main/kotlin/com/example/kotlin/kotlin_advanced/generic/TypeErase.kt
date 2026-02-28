package com.example.kotlin.kotlin_advanced.generic

// 타입소거
class TypeErase {

    fun checkStringList(data: Any) {
        // Error: Cannot check for instance of erased type 'List<String>'.
//        if (data is List<String>) {
//
//        }

        // Star Projection
        if (data is List<*>) {
            val elements: Any? = data[0]
        }
    }

    fun checkMutableList(data: Collection<String>) {
        if (data is MutableList<String>) {
            data.add("Mutable")
        }
    }

    fun <T> T.toSuperString() {
        // T가 무엇인지 런탕미 때도 알 수 없기 때문에 오류가 난다.
        // print("${T::class.java.name}: $this")
    }
}

inline fun <reified T>List<*>.hasAnyInstanceOf(): Boolean {
    return any { it is T }
}

inline fun <reified T> T.toSuperString() {
     print("${T::class.java.name}: $this")
}

fun main() {
    val numbers = listOf(1, 2f, 3.0)
    numbers.filterIsInstance<Float>() // [2f]
}