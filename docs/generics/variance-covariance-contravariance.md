# Kotlin 공변성(Covariance)과 반공변성(Contravariance) 깊이 이해하기

## 왜 변성(Variance)이 필요한가?

변성을 이해하려면 먼저 **"타입 안전성과 유연성 사이의 긴장"** 을 이해해야 합니다.

### 문제 상황

```kotlin
open class Animal
class Cat : Animal()
class Dog : Animal()
```

우리는 `Cat`이 `Animal`의 서브타입이라는 것을 알고 있습니다.
그렇다면 `List<Cat>`은 `List<Animal>`의 서브타입일까요?

```kotlin
val cats: List<Cat> = listOf(Cat(), Cat())
val animals: List<Animal> = cats  // 이게 가능할까?
```

**직관적으로는 "당연히 되어야지!"** 라고 생각합니다.
고양이 리스트는 동물 리스트의 일종이니까요.

하지만 만약 리스트에 **쓰기**가 가능하다면?

```kotlin
val cats: MutableList<Cat> = mutableListOf(Cat(), Cat())
val animals: MutableList<Animal> = cats  // 만약 이게 된다면...
animals.add(Dog())  // 동물 리스트에 개를 넣음 - 합법적으로 보임
val cat: Cat = cats[2]  // 그런데 cats에서 꺼내면... Dog이 나옴! 런타임 에러!
```

이것이 바로 **변성이 필요한 이유**입니다.
- **읽기만 한다면** → `List<Cat>`을 `List<Animal>`로 안전하게 사용 가능
- **쓰기도 한다면** → 타입 안전성이 깨질 수 있음

---

## 세 가지 변성

| 변성 | 키워드 | 타입 관계 | 비유 |
|------|--------|-----------|------|
| **공변성** (Covariance) | `out T` | `Cat <: Animal` → `Box<Cat> <: Box<Animal>` | 같은 방향 |
| **반공변성** (Contravariance) | `in T` | `Cat <: Animal` → `Box<Animal> <: Box<Cat>` | 반대 방향 |
| **불변성** (Invariance) | `T` | `Box<Cat>`과 `Box<Animal>`은 무관 | 관계 없음 |

> `<:` 는 "~의 서브타입" 이라는 의미입니다.

---

## 1. 공변성 (Covariance) - `out T`

### 핵심 직관: "꺼내기만 하면 안전하다"

```kotlin
// out = T를 밖으로(out) 내보내기만 한다
interface Producer<out T> {
    fun produce(): T        // T를 반환 (밖으로 나감) → 허용
    // fun consume(item: T) // T를 받음 (안으로 들어옴) → 컴파일 에러!
}
```

### 왜 안전한가?

상자에서 **꺼내기만** 하는 상황을 생각해봅시다:

```kotlin
class AnimalShelter<out T>(private val animal: T) {
    fun getAnimal(): T = animal  // 꺼내기만 함
}

val catShelter: AnimalShelter<Cat> = AnimalShelter(Cat())
val animalShelter: AnimalShelter<Animal> = catShelter  // 안전!

val animal: Animal = animalShelter.getAnimal()  // Cat이 나오지만, Animal로 받으면 OK
```

왜 안전할까요?
- `catShelter`에서 나오는 것은 반드시 `Cat`
- `Cat`은 `Animal`의 서브타입
- 따라서 `Animal` 타입으로 받아도 아무 문제 없음

### 실생활 비유

> **자판기(Vending Machine)**를 생각하세요.
>
> 콜라 자판기(`Producer<Cola>`)는 음료 자판기(`Producer<Drink>`)로 취급해도 됩니다.
> 콜라 자판기에서 나오는 건 항상 콜라이고, 콜라는 음료의 일종이니까요.
>
> 하지만 아무 음료나 **넣을 수 있는** 기계라면? 콜라 자판기에 사이다를 넣을 수 없으니 위험합니다.

### Kotlin 표준 라이브러리 예시

```kotlin
// List는 out E → 공변
public interface List<out E> : Collection<E> {
    operator fun get(index: Int): E      // E를 반환만 함
    // add(element: E)는 없음! → 읽기 전용
}

// 따라서 이게 가능
val strings: List<String> = listOf("A", "B")
val anys: List<Any> = strings  // String <: Any → List<String> <: List<Any>
```

```kotlin
// MutableList는 out이 없음 → 불변
public interface MutableList<E> : List<E> {
    fun add(element: E): Boolean  // E를 받음 → out 불가
    operator fun get(index: Int): E
}

// 따라서 이건 불가능
val mutableStrings: MutableList<String> = mutableListOf("A")
// val mutableAnys: MutableList<Any> = mutableStrings  // 컴파일 에러!
```

---

## 2. 반공변성 (Contravariance) - `in T`

### 핵심 직관: "넣기만 하면 안전하다 (방향이 뒤집힌다)"

```kotlin
// in = T를 안으로(in) 받기만 한다
interface Consumer<in T> {
    fun consume(item: T)    // T를 받음 (안으로 들어옴) → 허용
    // fun produce(): T     // T를 반환 (밖으로 나감) → 컴파일 에러!
}
```

### 왜 방향이 뒤집히는가?

이 부분이 가장 혼란스러운 부분입니다. 천천히 살펴봅시다.

```kotlin
// "모든 동물"을 처리할 수 있는 수의사
class AnimalVet : Consumer<Animal> {
    override fun consume(item: Animal) {
        println("${item::class.simpleName} 치료 중")
    }
}

val animalVet: Consumer<Animal> = AnimalVet()

// 질문: 이 수의사를 "고양이 전문 수의사"로 쓸 수 있을까?
val catVet: Consumer<Cat> = animalVet  // 가능! 반공변성
```

왜 가능할까요?
- `animalVet`은 **모든 동물**을 치료할 수 있음
- 고양이도 동물의 일종
- 따라서 **고양이만 받는 곳**에 `animalVet`을 써도 안전

반대는?

```kotlin
// "고양이만" 치료할 수 있는 수의사
class CatVet : Consumer<Cat> {
    override fun consume(item: Cat) {
        println("고양이 ${item} 치료 중")
    }
}

val catVet: Consumer<Cat> = CatVet()
// val animalVet: Consumer<Animal> = catVet  // 컴파일 에러!
// 왜? 고양이 전문 수의사에게 개를 데려가면 치료 못함!
```

### 타입 관계가 뒤집히는 이유 정리

```
타입 계층:        Cat  <:  Animal       (Cat은 Animal의 서브타입)
                   ↓        ↓
공변 (out):   Box<Cat> <: Box<Animal>   (같은 방향)
반공변 (in):  Box<Animal> <: Box<Cat>   (반대 방향!)
```

**왜 반대 방향일까?**

"처리할 수 있는 범위"로 생각하면 됩니다:
- `Consumer<Animal>`은 모든 동물을 처리 가능 → **더 범용적**
- `Consumer<Cat>`은 고양이만 처리 가능 → **더 제한적**

**범용적인 것을 제한적인 곳에 쓸 수 있지만, 그 반대는 안 됩니다.**

### 실생활 비유

> **쓰레기통**을 생각하세요.
>
> "모든 쓰레기"를 받는 일반 쓰레기통(`Consumer<Trash>`)은
> "음식물 쓰레기만" 받는 곳(`Consumer<FoodWaste>`)에서도 사용 가능합니다.
>
> 하지만 "음식물 쓰레기만" 받는 통에 일반 쓰레기를 넣을 순 없습니다.

### Kotlin 표준 라이브러리 예시

```kotlin
// Comparable<in T> → Kotlin에서 선언 지점 반공변
public interface Comparable<in T> {
    operator fun compareTo(other: T): Int  // T를 받기만 함
}

// Number 비교기를 Int 비교기로 사용 가능
val numberComparable: Comparable<Number> = object : Comparable<Number> {
    override fun compareTo(other: Number): Int = 0
}

val intComparable: Comparable<Int> = numberComparable  // 반공변!
```

> **주의: `Comparator` vs `Comparable`**
>
> `kotlin.Comparable<in T>`은 Kotlin 자체 인터페이스라서 `in` 변성이 적용됩니다.
> 하지만 `kotlin.Comparator<T>`는 `java.util.Comparator<T>`의 **typealias**입니다.
> Java 인터페이스에는 선언 지점 변성이 없으므로 `Comparator<Number>`를 `Comparator<Int>`에
> 직접 할당할 수 없습니다.
>
> ```kotlin
> // Comparable → Kotlin 인터페이스 → in 변성 적용 → 할당 가능
> val nc: Comparable<Number> = ...
> val ic: Comparable<Int> = nc  // OK!
>
> // Comparator → Java typealias → 변성 없음 → 할당 불가
> val nc2: Comparator<Number> = Comparator { a, b -> ... }
> // val ic2: Comparator<Int> = nc2  // 컴파일 에러!
> ```

```kotlin
// 함수 타입에서의 반공변성
// (Animal) -> String 을 (Cat) -> String 으로 사용 가능

val describeAnimal: (Animal) -> String = { animal -> "동물: ${animal::class.simpleName}" }
val describeCat: (Cat) -> String = describeAnimal  // 반공변!
// 왜? 모든 동물을 설명할 수 있으면, 고양이도 당연히 설명 가능
```

---

## 3. 함수 타입에서의 변성 (가장 실용적)

Kotlin의 함수 타입 `(P) -> R`은 사실 내부적으로 이렇게 되어 있습니다:

```kotlin
// 파라미터(P)는 in (반공변), 반환(R)은 out (공변)
interface Function1<in P, out R> {
    operator fun invoke(p: P): R
}
```

### 왜 이렇게 될까?

```kotlin
open class Animal { fun breathe() = "숨쉬기" }
class Cat : Animal() { fun meow() = "야옹" }
class Dog : Animal() { fun bark() = "멍멍" }

// 함수 타입의 변성을 단계별로 이해해봅시다

// 1) 반환 타입은 공변 (out)
val getCat: () -> Cat = { Cat() }
val getAnimal: () -> Animal = getCat  // Cat을 반환하지만 Animal로 받아도 OK
// 꺼내기만 하니까 안전!

// 2) 파라미터 타입은 반공변 (in)
val feedAnimal: (Animal) -> String = { "${it::class.simpleName}에게 밥주기" }
val feedCat: (Cat) -> String = feedAnimal  // 모든 동물에게 밥 줄 수 있으면 고양이에게도 가능
// 넣기만 하니까 안전!

// 3) 조합
val processAnimalToCat: (Animal) -> Cat = { Cat() }  // 동물을 받아서 고양이를 반환
val processCatToAnimal: (Cat) -> Animal = processAnimalToCat  // 둘 다 적용 가능!
```

### 왜 이 순서인가?

```
파라미터: Cat → Animal (반공변 - 더 넓은 타입 허용 = 더 범용적)
             ↓
            함수 호출
             ↓
반환값:   Cat → Animal (공변 - 더 구체적인 타입을 더 넓은 타입으로)
```

---

## 4. 선언 지점 변성 vs 사용 지점 변성

### 선언 지점 변성 (Declaration-site variance)

클래스를 **정의할 때** 변성을 지정합니다. Kotlin의 방식입니다.

```kotlin
// 클래스 선언에서 out/in 지정
class ImmutableBox<out T>(private val value: T) {
    fun get(): T = value
}

class Processor<in T> {
    fun process(item: T) { println(item) }
}
```

### 사용 지점 변성 (Use-site variance)

함수나 변수를 **사용할 때** 변성을 지정합니다. Java의 와일드카드(`? extends`, `? super`)와 유사합니다.

```kotlin
// 불변인 MutableList에 사용 지점 변성 적용
fun copy(from: MutableList<out Animal>, to: MutableList<Animal>) {
    // from은 out → 읽기만 가능
    for (animal in from) {
        to.add(animal)
    }
    // from.add(Cat())  // 컴파일 에러! out이니까 쓰기 불가
}

fun addCats(list: MutableList<in Cat>) {
    // list는 in → 쓰기만 가능 (Cat 또는 Cat의 서브타입만)
    list.add(Cat())
    // val cat: Cat = list[0]  // 컴파일 에러! in이니까 읽기 시 Any?만 가능
}
```

### Java와의 비교

| Kotlin | Java | 의미 |
|--------|------|------|
| `out T` | `? extends T` | 공변 (T 또는 T의 서브타입) |
| `in T` | `? super T` | 반공변 (T 또는 T의 슈퍼타입) |
| `*` | `?` | 스타 프로젝션 (알 수 없는 타입) |

### PECS 원칙 (Producer Extends, Consumer Super)

Java에서 유명한 원칙이며, Kotlin에서는 더 직관적입니다:

```
Java:   Producer → ? extends T  /  Consumer → ? super T
Kotlin: Producer → out T        /  Consumer → in T
```

---

## 5. 실전 종합 예제

### 이벤트 시스템

```kotlin
open class Event(val timestamp: Long = System.currentTimeMillis())
class ClickEvent(val x: Int, val y: Int) : Event()
class KeyEvent(val keyCode: Int) : Event()

// 이벤트 발생기 - 공변 (이벤트를 생산)
interface EventSource<out T : Event> {
    fun nextEvent(): T
    fun recentEvents(): List<T>
}

// 이벤트 처리기 - 반공변 (이벤트를 소비)
interface EventHandler<in T : Event> {
    fun handle(event: T)
}

// 이벤트 변환기 - 반공변+공변 조합
interface EventTransformer<in I : Event, out O : Event> {
    fun transform(input: I): O
}
```

```kotlin
// 구현
class ClickEventSource : EventSource<ClickEvent> {
    override fun nextEvent() = ClickEvent(100, 200)
    override fun recentEvents() = listOf(ClickEvent(0, 0))
}

class GeneralEventHandler : EventHandler<Event> {
    override fun handle(event: Event) {
        println("이벤트 처리: ${event::class.simpleName} at ${event.timestamp}")
    }
}

// 사용
fun processClick(source: EventSource<ClickEvent>, handler: EventHandler<ClickEvent>) {
    val event = source.nextEvent()
    handler.handle(event)
}

val clickSource: EventSource<ClickEvent> = ClickEventSource()
val generalHandler: EventHandler<Event> = GeneralEventHandler()

// 공변성: EventSource<ClickEvent>는 EventSource<Event>의 서브타입
val eventSource: EventSource<Event> = clickSource  // OK!

// 반공변성: EventHandler<Event>는 EventHandler<ClickEvent>의 서브타입
val clickHandler: EventHandler<ClickEvent> = generalHandler  // OK!

processClick(clickSource, generalHandler)  // 둘 다 적용!
```

### 저장소 패턴

```kotlin
// 읽기 전용 저장소 - 공변
interface ReadRepository<out T> {
    fun findById(id: Long): T?
    fun findAll(): List<T>
}

// 쓰기 전용 저장소 - 반공변
interface WriteRepository<in T> {
    fun save(entity: T)
    fun delete(entity: T)
}

// 읽기/쓰기 저장소 - 불변 (out/in 모두 필요하므로)
interface Repository<T> : ReadRepository<T>, WriteRepository<T>
```

```kotlin
open class Entity(val id: Long)
class User(id: Long, val name: String) : Entity(id)

class UserRepository : Repository<User> {
    private val store = mutableMapOf<Long, User>()

    override fun findById(id: Long): User? = store[id]
    override fun findAll(): List<User> = store.values.toList()
    override fun save(entity: User) { store[entity.id] = entity }
    override fun delete(entity: User) { store.remove(entity.id) }
}

// 공변성 활용: UserRepository의 읽기 부분을 Entity 레벨로 사용
val userRepo = UserRepository()
val entityReader: ReadRepository<Entity> = userRepo  // out 덕분에 가능!

// 반공변성 활용: 더 넓은 타입의 쓰기를 더 좁은 곳에
val entityWriter: WriteRepository<Entity> = object : WriteRepository<Entity> {
    override fun save(entity: Entity) { println("Save: ${entity.id}") }
    override fun delete(entity: Entity) { println("Delete: ${entity.id}") }
}
val userWriter: WriteRepository<User> = entityWriter  // in 덕분에 가능!
```

---

## 6. 흔한 혼동 정리

### Q1: "out이면 왜 파라미터에 쓸 수 없나요?"

```kotlin
class Box<out T>(private val value: T) {
    fun get(): T = value          // OK: T가 밖으로 나감
    // fun set(item: T) { ... }   // 에러: T가 안으로 들어옴
}
```

만약 `set`이 가능하다면:

```kotlin
val catBox: Box<Cat> = Box(Cat())
val animalBox: Box<Animal> = catBox   // 공변이니까 가능
animalBox.set(Dog())                   // Animal 자리에 Dog 넣기 → 합법적으로 보임
val cat: Cat = catBox.get()            // 하지만 원래 catBox에서 꺼내면 Dog이 나옴!
```

**타입 안전성이 깨지므로 컴파일러가 막는 것입니다.**

### Q2: "in이면 왜 반환에 쓸 수 없나요?"

```kotlin
class Sink<in T> {
    fun put(item: T) { ... }     // OK: T가 안으로 들어옴
    // fun get(): T { ... }       // 에러: T가 밖으로 나감
}
```

만약 `get`이 가능하다면:

```kotlin
val animalSink: Sink<Animal> = Sink<Animal>()
val catSink: Sink<Cat> = animalSink  // 반공변이니까 가능
animalSink.put(Dog())                 // Animal을 넣기 → OK
val cat: Cat = catSink.get()          // 하지만 Cat으로 꺼내면 Dog이 나옴!
```

### Q3: "공변이면 생성자 파라미터는 괜찮은가요?"

```kotlin
// 이건 됩니다! val이면 getter만 생기므로
class Box<out T>(val value: T)  // OK

// 이건 안 됩니다! var이면 setter도 생기므로
// class Box<out T>(var value: T)  // 컴파일 에러

// private이면 됩니다! 외부에 노출되지 않으므로
class Box<out T>(private var value: T) {
    fun get(): T = value  // OK
}
```

---

## 7. 한눈에 보는 정리표

```
┌─────────────────────────────────────────────────────────────┐
│                    변성(Variance) 정리                        │
├──────────┬──────────────────┬───────────────────────────────┤
│          │  공변 (out T)     │  반공변 (in T)                │
├──────────┼──────────────────┼───────────────────────────────┤
│ 역할     │  생산자 (Producer) │  소비자 (Consumer)            │
│ T 위치   │  반환 타입만       │  파라미터 타입만               │
│ 방향     │  같은 방향 유지    │  방향 뒤집힘                   │
│ 비유     │  자판기 (꺼내기)   │  쓰레기통 (넣기)              │
│ 키워드   │  out              │  in                           │
│ Java     │  ? extends T      │  ? super T                    │
│ 읽기     │  가능 (T로)       │  불가 (Any?로만)              │
│ 쓰기     │  불가             │  가능 (T 또는 서브타입)        │
├──────────┼──────────────────┴───────────────────────────────┤
│ 예시     │  List<out E>         Comparable<in T>             │
│          │  Result<out T>       EventHandler<in T>           │
│          │  Iterator<out T>     (T) -> R 의 T 부분           │
└──────────┴──────────────────────────────────────────────────┘

타입 관계:
  Cat <: Animal (Cat은 Animal의 서브타입)

  공변:   Producer<Cat>  <: Producer<Animal>   (같은 방향)
  반공변: Consumer<Animal> <: Consumer<Cat>    (반대 방향)
  불변:   MutableList<Cat> ≠ MutableList<Animal> (관계 없음)
```

---

## 관련 파일

- [generics.md](./generics.md) - 제네릭 기본 가이드
- 예제 코드: `src/main/kotlin/com/example/kotlin/kotlin_advanced/variance/`
- [Kotlin 공식 문서 - Generics: in, out, where](https://kotlinlang.org/docs/generics.html)
