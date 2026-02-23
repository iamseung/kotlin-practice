package com.example.kotlin.kotlin_advanced.generic

// 타입 파라미터 T
class Cage2<T> {
    private val animals: MutableList<T> = mutableListOf()

    fun getFirst(): T {
        return animals.first()
    }

    fun put(animal: T) {
        animals.add(animal)
    }

    fun moveFrom(cage: Cage2<T>) {
        this.animals.addAll(cage.animals)
    }
}