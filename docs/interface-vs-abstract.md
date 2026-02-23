# Interface vs Abstract Class

## 핵심 차이

| 항목 | Interface | Abstract Class |
|------|-----------|----------------|
| 다중 구현 | 여러 개 구현 가능 | 단일 상속만 가능 |
| 생성자 | 불가 | 가능 |
| 상태(필드) | 상태 저장 불가 (추상 프로퍼티만) | 상태 저장 가능 |
| 기본 구현 | 제공 가능 | 제공 가능 |
| 목적 | **할 수 있는 것** (능력/계약) | **무엇인가** (공통 베이스) |

---

## Interface

```kotlin
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

// 여러 인터페이스 동시 구현 가능
class Duck : Flyable, Swimmable {
    override val maxAltitude = 100

    override fun fly() = println("오리가 날아요")
    override fun swim() = println("오리가 헤엄쳐요")
}
```

**인터페이스는 상태를 저장할 수 없다:**

```kotlin
interface Counter {
    var count: Int  // 실제 필드가 아닌 추상 프로퍼티 — 구현체가 backing field를 가짐
}
```

---

## Abstract Class

```kotlin
abstract class Animal(
    val name: String,       // 생성자 파라미터 가능
    private var energy: Int // 상태(필드) 저장 가능
) {
    abstract fun sound(): String  // 반드시 구현

    fun eat(amount: Int) {        // 공통 구현 — 오버라이드 선택
        energy += amount
        println("$name 이(가) 먹었습니다. 에너지: $energy")
    }
}

class Dog(name: String) : Animal(name, energy = 100) {
    override fun sound() = "멍멍"
}

class Cat(name: String) : Animal(name, energy = 80) {
    override fun sound() = "야옹"
}
```

---

## 함께 사용하기

```kotlin
abstract class Vehicle(val brand: String) {
    abstract fun drive()
}

interface Electric {
    fun charge()
}

interface Autonomous {
    fun selfDrive()
}

// 추상 클래스 상속 + 여러 인터페이스 구현
class TeslaModel3 : Vehicle("Tesla"), Electric, Autonomous {
    override fun drive() = println("Tesla 주행")
    override fun charge() = println("충전 중")
    override fun selfDrive() = println("자율주행 중")
}
```

---

## 선택 기준

```
"IS-A" 관계이고 공유 상태/생성자 로직이 필요한가?
  → Abstract Class

"CAN-DO" 관계이거나 다중 구현이 필요한가?
  → Interface
```

**Interface 선택:**
- 서로 다른 계층의 클래스에 공통 능력 부여 (`Serializable`, `Comparable`)
- 다중 구현이 필요할 때

**Abstract Class 선택:**
- 공통 상태(필드)나 초기화 로직이 필요할 때
- 템플릿 메서드 패턴처럼 실행 흐름의 뼈대를 정의할 때
