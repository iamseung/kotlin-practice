package com.example.kotlin.kotlin_advanced.generic

/*
타입 파라미터 T에 제약을 걸고 싶다.
즉, T에 Animal의 하위타입만 받게 하고 싶은 상황.
`class Cage5<T : Animal>` -> 이런 식으로 제약을 걸 수 있음.

추가로 Animal 하위타입으로 제한 + Comparable 구현이 필요하다면
`class Cage5<T> where T : Animal, T : Comparable<T>` -> where 이라는 키워드로 제한 조건을 걸 수 있음.
 */

fun main() {
//    Cage5<Int>()
//    Cage5<String>()
    val cage = Cage5(mutableListOf(Eagle(), Sparrow()))
    cage.printAfterSorting()
}

abstract class Bird(
    name: String,
    private val size: Int,
) : Animal(name), Comparable<Bird> {
    override fun compareTo(other: Bird): Int {
        return size.compareTo(other.size)
    }
}

class Sparrow: Bird("참새", 100)
class Eagle: Bird("독수리", 500)

class Cage5<T>(
    private val animals: MutableList<T> = mutableListOf()
) where T : Animal, T : Comparable<T> {
    fun printAfterSorting() {
        this.animals.sorted()
            .map { it.name }
            .let { print(it) }
    }

    fun getFirst(): T {
        return animals.first()
    }

    fun put(animal: T) {
        animals.add(animal)
    }

    fun moveFrom(otherCage: Cage5<T>) {
        this.animals.addAll(otherCage.animals)
    }

    fun moveTo(otherCage: Cage5<T>) {
        otherCage.animals.addAll(this.animals)
    }
}

/*
  ┌────────────────┬──────────────────────────────────────────┐
  │      위치       │                   역할                    │
  ├────────────────┼──────────────────────────────────────────┤
  │ fun <T>        │ T를 선언 (이 함수에서 T를 쓰겠다고 등록)         │
  ├────────────────┼──────────────────────────────────────────┤
  │ List<T>        │ T를 사용 (수신 객체의 원소 타입이 T)            │
  ├────────────────┼──────────────────────────────────────────┤
  │ other: List<T> │ T를 사용 (파라미터도 같은 T여야 함)             │
  └────────────────┴──────────────────────────────────────────┘

[함수 레벨 제네릭]
클래스가 아닌 함수 자체에 타입 파라미터 <T>를 선언할 수 있다.
`fun <T>` : 이 함수가 호출될 때 T가 결정되며, 클래스의 T와는 독립적이다.

`List<T>.hasIntersection` : List<T>의 확장 함수로, 수신 객체(this)의 원소 타입이 T다.
`other: List<T>` : 파라미터도 동일한 T를 사용하므로, this와 other는 반드시 같은 타입의 리스트여야 한다.
예) List<String>.hasIntersection(other: List<String>) -> OK
   List<String>.hasIntersection(other: List<Int>)    -> 컴파일 에러 (T가 달라서)

호출 시 T는 컴파일러가 인자를 보고 자동 추론한다. 명시적으로 쓰려면:
listOf("a").hasIntersection<String>(listOf("a", "b"))
 */

fun <T> List<T>.hasIntersection(other: List<T>): Boolean {
    return (this.toSet() intersect other.toSet()).isNotEmpty()
}

fun test() {
    val list1 = listOf(1, 2, 3)
    list1.hasIntersection(listOf("1", "2", "3"))
}