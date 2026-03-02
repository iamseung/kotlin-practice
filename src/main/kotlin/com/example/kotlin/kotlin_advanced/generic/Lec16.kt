package com.example.kotlin.kotlin_advanced.generic

fun main() {
    val filter: StringFilter = object : StringFilter {
        override fun predicate(str: String?): Boolean {
            return str?.startsWith("A") ?: false
        }
    }

    // SAM 생성자
    val filter2 = StringFilter { s -> s.startsWith("A") }

    consumeFilter({ s -> s.startsWith("A") })

    KStringFilter { it.startsWith("A") }
}

fun consumeFilter(filter: StringFilter) {

}

fun interface KStringFilter {
    fun predicate(str: String): Boolean
}