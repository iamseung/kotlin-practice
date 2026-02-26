package com.example.kotlin.kotlin_advanced.generic.variance

/**
 * ==============================================
 *  공변성(Covariance)과 반공변성(Contravariance)
 *  실행 가능한 예제 모음
 * ==============================================
 */

// ──────────────────────────────────
// 1. 기본 타입 계층
// ──────────────────────────────────

open class Animal(val name: String) {
    override fun toString() = "${this::class.simpleName}($name)"
}

open class Cat(name: String) : Animal(name) {
    fun meow() = "$name: 야옹~"
}

class PersianCat(name: String) : Cat(name)

open class Dog(name: String) : Animal(name) {
    fun bark() = "$name: 멍멍!"
}

// ──────────────────────────────────
// 2. 공변성 (out T) - 생산자
// ──────────────────────────────────

/**
 * out T: T를 반환(생산)만 하는 인터페이스
 *
 * 핵심: "꺼내기만 하면 안전하다"
 * - Cat을 꺼내서 Animal로 받으면? → 안전 (Cat은 Animal이니까)
 * - 따라서 Producer<Cat>을 Producer<Animal>로 취급 가능
 */
interface Producer<out T> {
    fun produce(): T
    // fun consume(item: T)  // 컴파일 에러! out 위치에서 in 위치로 사용 불가
}

class CatProducer : Producer<Cat> {
    override fun produce(): Cat = Cat("나비")
}

class DogProducer : Producer<Dog> {
    override fun produce(): Dog = Dog("바둑이")
}

// ──────────────────────────────────
// 3. 반공변성 (in T) - 소비자
// ──────────────────────────────────

/**
 * in T: T를 파라미터(소비)로만 받는 인터페이스
 *
 * 핵심: "넣기만 하면 안전하다 (방향이 뒤집힌다)"
 * - Animal을 처리할 수 있는 핸들러는 Cat도 처리 가능
 * - 따라서 Consumer<Animal>을 Consumer<Cat>으로 취급 가능
 * - 방향이 뒤집힘!
 */
interface Consumer<in T> {
    fun consume(item: T)
    // fun produce(): T  // 컴파일 에러! in 위치에서 out 위치로 사용 불가
}

class AnimalPrinter : Consumer<Animal> {
    override fun consume(item: Animal) {
        println("  동물 출력: $item")
    }
}

class CatPrinter : Consumer<Cat> {
    override fun consume(item: Cat) {
        println("  고양이 출력: ${item.meow()}")
    }
}

// ──────────────────────────────────
// 4. 불변성 (T) - 읽기/쓰기 모두
// ──────────────────────────────────

/**
 * T (변성 없음): 읽기와 쓰기 모두 하므로 타입 변환 불가
 *
 * MutableList<Cat>을 MutableList<Animal>로 쓸 수 없는 이유:
 * - 만약 가능하다면 Dog을 넣을 수 있게 되어 타입 안전성이 깨짐
 */
class MutableBox<T>(var value: T) {
    fun get(): T = value        // T를 반환 (out 역할)
    fun set(item: T) {          // T를 받음 (in 역할)
        value = item
    }
}

// ──────────────────────────────────
// 5. 실전 예제: 이벤트 시스템
// ──────────────────────────────────

open class Event(val timestamp: Long = System.currentTimeMillis()) {
    override fun toString() = "${this::class.simpleName}(t=$timestamp)"
}

class ClickEvent(val x: Int, val y: Int) : Event() {
    override fun toString() = "ClickEvent(x=$x, y=$y)"
}

class KeyEvent(val key: Char) : Event() {
    override fun toString() = "KeyEvent(key=$key)"
}

// 이벤트 소스 - 공변 (이벤트를 생산)
interface EventSource<out T : Event> {
    fun nextEvent(): T
}

// 이벤트 핸들러 - 반공변 (이벤트를 소비)
interface EventHandler<in T : Event> {
    fun handle(event: T)
}

class ClickEventSource : EventSource<ClickEvent> {
    override fun nextEvent() = ClickEvent(100, 200)
}

class GeneralEventHandler : EventHandler<Event> {
    override fun handle(event: Event) {
        println("  이벤트 처리: $event")
    }
}

// ──────────────────────────────────
// 6. 실전 예제: 결과 타입 (sealed class + Nothing)
// ──────────────────────────────────

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

// ──────────────────────────────────
// main - 모든 예제 실행
// ──────────────────────────────────

fun main() {
    println("=" .repeat(60))
    println(" 공변성과 반공변성 예제")
    println("=".repeat(60))

    covarianceExample()
    contravarianceExample()
    invarianceExample()
    functionTypeVarianceExample()
    useSiteVarianceExample()
    eventSystemExample()
    resultWithNothingExample()
}

// ──────────────────────────────────
// 예제 1: 공변성 (out)
// ──────────────────────────────────
fun covarianceExample() {
    println("\n[1] 공변성 (out T) - 생산자")
    println("-".repeat(40))

    val catProducer: Producer<Cat> = CatProducer()

    // 핵심: Producer<Cat>을 Producer<Animal>에 할당 가능!
    // Cat <: Animal → Producer<Cat> <: Producer<Animal> (같은 방향)
    val animalProducer: Producer<Animal> = catProducer
    val animal: Animal = animalProducer.produce()
    println("  catProducer를 animalProducer로 사용: $animal")

    // List도 공변 (List<out E>)
    val cats: List<Cat> = listOf(Cat("나비"), Cat("야옹이"))
    val animals: List<Animal> = cats  // List<Cat> → List<Animal> 가능!
    println("  cats를 animals로 사용: $animals")

    // 왜 안전한가?
    // animals에서 꺼내면 Animal이 나오고, 실제로는 Cat → Cat은 Animal이므로 OK
    val firstAnimal: Animal = animals[0]
    println("  animals[0] = $firstAnimal (실제로는 Cat)")
}

// ──────────────────────────────────
// 예제 2: 반공변성 (in)
// ──────────────────────────────────
fun contravarianceExample() {
    println("\n[2] 반공변성 (in T) - 소비자")
    println("-".repeat(40))

    val animalPrinter: Consumer<Animal> = AnimalPrinter()

    // 핵심: Consumer<Animal>을 Consumer<Cat>에 할당 가능!
    // Cat <: Animal → Consumer<Animal> <: Consumer<Cat> (반대 방향!)
    val catConsumer: Consumer<Cat> = animalPrinter
    catConsumer.consume(Cat("나비"))

    println()
    println("  왜 안전한가?")
    println("  animalPrinter는 모든 Animal을 처리 가능")
    println("  Cat도 Animal이므로, Cat을 넣어도 안전!")

    // 반대는 불가능:
    // val catPrinter: Consumer<Cat> = CatPrinter()
    // val animalConsumer: Consumer<Animal> = catPrinter  // 컴파일 에러!
    // 왜? catPrinter는 Cat만 처리 가능, Dog을 넣으면 문제!
    println()
    println("  반대(Consumer<Cat> → Consumer<Animal>)는 불가능!")
    println("  고양이 전문 수의사에게 개를 데려갈 수 없음")

    // Comparable<in T>은 반공변
    // 주의: kotlin.Comparator는 java.util.Comparator의 typealias라서
    //       선언 지점 변성이 적용되지 않음. 사용 지점 변성으로 우회 필요.
    val numberComparable: Comparable<Number> = object : Comparable<Number> {
        override fun compareTo(other: Number): Int = other.toDouble().compareTo(0.0)
    }
    // Comparable<in T>이므로 Comparable<Number> → Comparable<Int> 가능
    val intComparable: Comparable<Int> = numberComparable  // 반공변!
    println()
    println("  Comparable<Number> → Comparable<Int>: ${intComparable.compareTo(5)}")

    // 우리가 만든 Consumer도 반공변 확인
    println("  Consumer<Animal> → Consumer<Cat>: 성공 (위에서 확인)")
}

// ──────────────────────────────────
// 예제 3: 불변성
// ──────────────────────────────────
fun invarianceExample() {
    println("\n[3] 불변성 (T) - 읽기/쓰기 모두")
    println("-".repeat(40))

    val catBox: MutableBox<Cat> = MutableBox(Cat("나비"))

    // 불변이므로 아래 두 가지 모두 불가:
    // val animalBox: MutableBox<Animal> = catBox  // 컴파일 에러!
    // val persianBox: MutableBox<PersianCat> = catBox  // 컴파일 에러!

    println("  MutableBox<Cat>은 MutableBox<Animal>에 할당 불가")
    println("  이유: set(Dog())을 호출하면 타입 안전성이 깨짐")

    // MutableList도 불변
    val mutableCats: MutableList<Cat> = mutableListOf(Cat("나비"))
    // val mutableAnimals: MutableList<Animal> = mutableCats  // 컴파일 에러!
    println("  MutableList<Cat>도 MutableList<Animal>에 할당 불가")

    // 같은 이유: 만약 가능하다면
    // mutableAnimals.add(Dog("바둑이"))  // Dog을 넣게 됨
    // val cat: Cat = mutableCats[1]       // Cat으로 꺼내면 Dog이 나옴!
}

// ──────────────────────────────────
// 예제 4: 함수 타입의 변성
// ──────────────────────────────────
fun functionTypeVarianceExample() {
    println("\n[4] 함수 타입의 변성")
    println("-".repeat(40))

    // 함수 타입 (P) -> R 에서:
    // P는 반공변 (in), R은 공변 (out)

    // 반환 타입 공변 예시
    val getCat: () -> Cat = { Cat("함수에서 생성된 고양이") }
    val getAnimal: () -> Animal = getCat  // () -> Cat을 () -> Animal로 사용 가능
    println("  반환 공변: ${getAnimal()}")

    // 파라미터 반공변 예시
    val describeAnimal: (Animal) -> String = { "동물: $it" }
    val describeCat: (Cat) -> String = describeAnimal  // (Animal) -> String을 (Cat) -> String으로 사용 가능
    println("  파라미터 반공변: ${describeCat(Cat("나비"))}")

    // 조합 예시
    val animalToCat: (Animal) -> Cat = { Cat("변환된_${it.name}") }
    val catToAnimal: (Cat) -> Animal = animalToCat  // 파라미터 반공변 + 반환 공변
    println("  조합: ${catToAnimal(Cat("원본"))}")
}

// ──────────────────────────────────
// 예제 5: 사용 지점 변성 (Use-site variance)
// ──────────────────────────────────
fun useSiteVarianceExample() {
    println("\n[5] 사용 지점 변성 (Use-site variance)")
    println("-".repeat(40))

    val cats: MutableList<Cat> = mutableListOf(Cat("나비"), Cat("야옹이"))
    val animals: MutableList<Animal> = mutableListOf()

    // MutableList는 불변이지만, 함수 파라미터에서 out/in을 지정 가능
    copyAnimals(from = cats, to = animals)
    println("  복사 후 animals: $animals")

    val moreCats: MutableList<in Cat> = mutableListOf<Animal>()
    addCats(moreCats)
    println("  addCats 후: $moreCats")
}

// from은 out → 읽기만 가능 (Cat 리스트에서 Animal로 꺼냄)
// to는 그대로 Animal 리스트
fun copyAnimals(from: MutableList<out Animal>, to: MutableList<Animal>) {
    for (animal in from) {
        to.add(animal)
    }
    // from.add(Cat("새 고양이"))  // 컴파일 에러! out이므로 쓰기 불가
}

// list는 in → 쓰기만 가능 (Cat을 넣음)
fun addCats(list: MutableList<in Cat>) {
    list.add(Cat("추가된 고양이1"))
    list.add(PersianCat("추가된 페르시안"))
    // val cat: Cat = list[0]  // 컴파일 에러! in이므로 Cat으로 읽기 불가 (Any?만 가능)
    val item: Any? = list[0]   // Any?로는 읽기 가능
    println("  list[0] as Any? = $item")
}

// ──────────────────────────────────
// 예제 6: 이벤트 시스템 (공변 + 반공변 조합)
// ──────────────────────────────────
fun eventSystemExample() {
    println("\n[6] 이벤트 시스템 (공변 + 반공변 조합)")
    println("-".repeat(40))

    val clickSource: EventSource<ClickEvent> = ClickEventSource()
    val generalHandler: EventHandler<Event> = GeneralEventHandler()

    // 공변: EventSource<ClickEvent> → EventSource<Event>
    val eventSource: EventSource<Event> = clickSource
    println("  공변 - clickSource를 eventSource로:")
    println("  ${eventSource.nextEvent()}")

    // 반공변: EventHandler<Event> → EventHandler<ClickEvent>
    val clickHandler: EventHandler<ClickEvent> = generalHandler
    println("  반공변 - generalHandler를 clickHandler로:")
    clickHandler.handle(ClickEvent(50, 75))

    // 실전 활용: 범용 처리 함수
    processEvents(clickSource, generalHandler)
}

fun <T : Event> processEvents(source: EventSource<T>, handler: EventHandler<T>) {
    val event = source.nextEvent()
    handler.handle(event)
}

// ──────────────────────────────────
// 예제 7: Result + Nothing (sealed class)
// ──────────────────────────────────
fun resultWithNothingExample() {
    println("\n[7] Result<out T> + Nothing")
    println("-".repeat(40))

    // Nothing은 모든 타입의 서브타입
    // Result<Nothing>은 Result<모든타입>의 서브타입 (out과 결합)

    val stringResult: Result<String> = Result.Success("안녕하세요")
    val intResult: Result<Int> = Result.Success(42)

    // Error와 Loading은 Result<Nothing>이지만 모든 Result<T>에 할당 가능
    val errorForString: Result<String> = Result.Error("문자열 에러")
    val errorForInt: Result<Int> = Result.Error("숫자 에러")
    val loadingForAnything: Result<Cat> = Result.Loading

    println("  Result<String> = Success: $stringResult")
    println("  Result<Int>    = Success: $intResult")
    println("  Result<String> = Error: $errorForString")
    println("  Result<Int>    = Error: $errorForInt")
    println("  Result<Cat>    = Loading: $loadingForAnything")

    // out 덕분에 Result<String>을 Result<Any>에 할당 가능
    val anyResult: Result<Any> = stringResult
    println("  Result<Any>    = (was String): $anyResult")

    // when 패턴 매칭
    println()
    printResult("stringResult", stringResult)
    printResult("errorForInt", errorForInt)
    printResult("loadingForCat", loadingForAnything)
}

fun <T> printResult(label: String, result: Result<T>) {
    when (result) {
        is Result.Success -> println("  $label → 성공: ${result.data}")
        is Result.Error -> println("  $label → 에러: ${result.message}")
        is Result.Loading -> println("  $label → 로딩 중...")
    }
}
