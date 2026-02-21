package com.example.kotlin.practice

import org.junit.jupiter.api.Test

// 기본 타입 별칭
typealias Name = String
typealias Age = Int
typealias PersonMap = Map<Name, Age>

// 컬렉션 타입 별칭
typealias FruitList = List<Fruit>

// 함수 타입 별칭
typealias FruitFilter = (Fruit) -> Boolean
typealias FruitTransform = (Fruit) -> String

data class Fruit(val name: String, val price: Int)

class TypedTest {

    @Test
    fun `typealias 기본 사용 예시`() {
        // Name, Age 는 사실상 String, Int 와 동일
        val name: Name = "Alice"
        val age: Age = 30

        val personMap: PersonMap = mapOf(name to age)
        println(personMap) // {Alice=30}
    }

    @Test
    fun `typealias 컬렉션 예시`() {
        val fruits: FruitList = listOf(
            Fruit("Apple", 1000),
            Fruit("Banana", 500),
            Fruit("Cherry", 2000)
        )
        println(fruits)
    }

    @Test
    fun `typealias 함수 타입 예시`() {
        // FruitFilter = (Fruit) -> Boolean
        val expensiveFilter: FruitFilter = { fruit -> fruit.price >= 1000 }

        // FruitTransform = (Fruit) -> String
        val nameTransform: FruitTransform = { fruit -> fruit.name.uppercase() }

        val fruits = listOf(
            Fruit("Apple", 1000),
            Fruit("Banana", 500),
            Fruit("Cherry", 2000)
        )

        val expensive = fruits.filter(expensiveFilter)
        val names = fruits.map(nameTransform)

        println("비싼 과일: $expensive")
        println("과일 이름 대문자: $names")
    }

    @Test
    fun `구조분해 테스트`() {
        val fruit = Fruit("Apple", 1000)
        val (name, price) = fruit
        // data class 는 ComponentN을 자동으로 만들어준다.
        // 위의 선언은 아래의 선언과 동일하다
        // 변수 순서대로 N
        /*
        val name = fruit.component1()
        val price = fruit.component2()
         */

        println("이름 : ${name}, 가격 : ${price}")
    }

    /*
    Component N은 연산자의 속성을 가지고 있다.
    "문법 → 함수" 매핑을 연산자 오버로딩이라고 하는데, operator 없이 그냥 fun component1()을 만들면 컴파일러가 이 함수를 구조분해 문법과 연결해주지 않는다.

    // operator 없으면 → 구조분해 불가, 일반 함수로만 호출 가능
    fun component1(): String = this.name

    // operator 있으면 → 구조분해 문법 사용 가능
    operator fun component1(): String = this.name

    즉, operator는 "이 함수를 해당 언어 문법과 연결해라" 라는 명시적인 선언입니다. data class가 구조분해를 자동으로 지원하는 이유도 컴파일러가 operator fun componentN()을 자동 생성해주기 때문!
     */
    class Person  (
        val name: String,
        val age: Int,
    ) {
        operator fun component1(): String {
            return this.name
        }

        operator fun component2(): Int {
            return this.age
        }
    }
}
