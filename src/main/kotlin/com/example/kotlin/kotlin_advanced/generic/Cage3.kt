package com.example.kotlin.kotlin_advanced.generic

// 오직 생산만 하는 클래스 -> 타입 파라미터가 반환 타입에만 사용된다!
class Cage3<out T> {
    private val animals: MutableList<T> = mutableListOf()

    fun getFirst(): T {
        return animals.first()
    }

    fun getAll(): List<T> {
        return animals
    }
}

fun main() {
    val fishCage: Cage3<Fish> = Cage3()
    val animalCage: Cage3<Animal> = fishCage
}