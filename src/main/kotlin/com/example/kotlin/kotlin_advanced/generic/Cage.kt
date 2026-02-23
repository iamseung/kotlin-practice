package com.example.kotlin.kotlin_advanced.generic

fun main() {
    val cage = Cage()
    cage.put(Carp("잉어"))

    // getFirst 시, Animal이 반환됨
    // val carp: Carp = cage.getFirst() // Error: Type Mismatch

    // 이렇게 할 경우 잘못된 타입을 반환하는 케이스가 발생할 수 있으며 컴파일 시점이 아닌 런타임 시점에 에러가 발생하게 되어 위험할 수 있음.
    // val carp: Carp = cage.getFirst() as Carp

    // as? : 형변환을 캐스팅 해주거나 실패하면 null 이 나옴
    val carp: Carp = cage.getFirst() as? Carp
        ?: throw IllegalArgumentException()


    val cage2 = Cage2<Carp>()
    cage2.put(Carp("잉어"))
    val carp2: Carp = cage2.getFirst()
}

class Cage {
    private val animals: MutableList<Animal> = mutableListOf()

    fun getFirst(): Animal {
        return animals.first()
    }

    fun put(animal: Animal) {
        animals.add(animal)
    }

    fun moveFrom(cage: Cage) {
        this.animals.addAll(cage.animals)
    }
}