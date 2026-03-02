package com.example.kotlin.clazz

interface Flyable {
    val maxAltitude: Int  // 구현체가 반드시 정의해야 함
    fun fly()             // 추상 메서드

    fun land() {          // 기본 구현 제공 가능
        println("착륙")
    }
}

interface Swimmable {
    fun swim()
}

class Duck : Flyable, Swimmable {
    override val maxAltitude = 100

    override fun fly() = println("오리가 날아요")
    override fun swim() = println("오리가 헤엄쳐요")
}

abstract class Animal(
    val name: String,       // 생성자 파라미터 가능
    private var energy: Int // 상태(필드) 저장 가능
) {
    abstract fun sound(): String  // 반드시 구현

    open fun eat(amount: Int) {        // 공통 구현 — 오버라이드 선택
        energy += amount
        println("$name 이(가) 먹었습니다. 에너지: $energy")
    }
}

class Dog(name: String) : Animal(name, energy = 100) {
    override fun sound() = "멍멍"

    override fun eat(amount: Int) {
        println(amount)
    }
}

class Cat(name: String) : Animal(name, energy = 80) {
    override fun sound() = "야옹"
}