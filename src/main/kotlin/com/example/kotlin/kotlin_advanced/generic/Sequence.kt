package com.example.kotlin.kotlin_advanced.generic

fun main() {
    val fruits = listOf(
        MyFruit("사과", 1000L),
        MyFruit("바나나", 2000L),
    )

    // 체이닝마다 중간 컬렉션(리스트)들이 계속 생성
    val avg = fruits
        .filter { it.name == "사과" }
        .map { it.price }
        .take(10_000)
        .average()

    // Sequence
    val avgWithSequence = fruits.asSequence()
        .filter { it.name == "사과" }
        .map { it.price }
        .take(10_000)
        .average()
}

data class MyFruit(
    val name: String,
    val price: Long,
)
