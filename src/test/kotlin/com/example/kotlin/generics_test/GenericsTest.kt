package com.example.kotlin.generics_test

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

/**
 * Generics (제네릭) - 타입 안전성을 보장하는 재사용 가능한 코드
 *
 * 핵심 개념:
 * 1. 제네릭 타입 파라미터: <T>, <E>, <K, V> 등
 * 2. Variance (변성):
 *    - out T (공변성, covariance): 생산자, 읽기만 가능
 *    - in T (반공변성, contravariance): 소비자, 쓰기만 가능
 *    - T (불변성, invariance): 읽기/쓰기 모두 가능
 * 3. Nothing 타입: 모든 타입의 서브타입, 값이 없음을 표현
 * 4. 타입 경계: <T : Number>, <T : Comparable<T>>
 */

// reified 타입 파라미터를 위한 헬퍼 함수들
inline fun <reified T> isInstance(value: Any): Boolean {
    return value is T
}

inline fun <reified T> parseJson(json: String): T? {
    return when (T::class) {
        String::class -> json as? T
        Int::class -> json.toIntOrNull() as? T
        else -> null
    }
}

class GenericsTest {

    /**
     * 예제 1: 기본 제네릭 클래스
     * T는 어떤 타입이든 올 수 있음
     */
    class Box<T>(val value: T) {
        fun get(): T = value

        fun <R> map(transform: (T) -> R): Box<R> {
            return Box(transform(value))
        }
    }

    @Test
    fun `제네릭 기본 - Box 타입`() {
        // Int 타입의 Box
        val intBox = Box(42)
        assertEquals(42, intBox.get())
        assertEquals(42, intBox.value)

        // String 타입의 Box
        val stringBox = Box("Hello")
        assertEquals("Hello", stringBox.get())

        // User 객체의 Box
        data class User(val name: String)
        val userBox = Box(User("John"))
        assertEquals("John", userBox.value.name)

        // map을 통한 타입 변환
        val lengthBox = stringBox.map { it.length }
        assertEquals(5, lengthBox.value)
    }

    /**
     * 예제 2: out T (공변성, Covariance)
     * "이 타입은 T를 생산(produce)만 합니다"
     * - T를 반환만 할 수 있음 (읽기 전용)
     * - T를 파라미터로 받을 수 없음 (쓰기 불가)
     * - 부모 타입을 자식 타입에 할당 가능
     */
    interface Producer<out T> {
        fun produce(): T
        // fun consume(item: T) // 컴파일 에러! out T는 파라미터로 사용 불가
    }

    class StringProducer : Producer<String> {
        override fun produce(): String = "Hello"
    }

    class NumberProducer : Producer<Number> {
        override fun produce(): Number = 42
    }

    @Test
    fun `out T - 공변성 이해하기`() {
        // String은 Any의 서브타입
        // Producer<String>은 Producer<Any>의 서브타입
        val stringProducer: Producer<String> = StringProducer()
        val anyProducer: Producer<Any> = stringProducer  // OK! 공변성

        assertEquals("Hello", anyProducer.produce())

        // 실용 예제: List<T>는 out T를 사용
        val strings: List<String> = listOf("A", "B", "C")
        val anys: List<Any> = strings  // OK! List는 out T

        assertEquals(3, anys.size)
    }

    /**
     * 예제 3: in T (반공변성, Contravariance)
     * "이 타입은 T를 소비(consume)만 합니다"
     * - T를 파라미터로만 받을 수 있음 (쓰기 전용)
     * - T를 반환할 수 없음 (읽기 불가, Any?만 가능)
     * - 자식 타입을 부모 타입에 할당 가능
     */
    interface Consumer<in T> {
        fun consume(item: T)
        // fun produce(): T // 컴파일 에러! in T는 반환 타입으로 사용 불가
    }

    class AnyConsumer : Consumer<Any> {
        val items = mutableListOf<Any>()
        override fun consume(item: Any) {
            items.add(item)
        }
    }

    @Test
    fun `in T - 반공변성 이해하기`() {
        // Any는 String의 슈퍼타입
        // Consumer<Any>는 Consumer<String>의 서브타입
        val anyConsumer: Consumer<Any> = AnyConsumer()
        val stringConsumer: Consumer<String> = anyConsumer  // OK! 반공변성

        stringConsumer.consume("Hello")
        stringConsumer.consume("World")

        // 실용 예제: MutableList.add는 in을 사용
        val anyList: MutableList<Any> = mutableListOf()
        anyList.add("String")
        anyList.add(123)
        anyList.add(3.14)

        assertEquals(3, anyList.size)
    }

    /**
     * 예제 4: Nothing 타입 이해하기
     * Nothing은 모든 타입의 서브타입
     * 값이 없음을 표현할 때 사용
     */
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val exception: Exception) : Result<Nothing>()  // Nothing 사용
        data object Loading : Result<Nothing>()  // Nothing 사용

        // Nothing을 사용하는 이유:
        // Error와 Loading은 data가 없으므로 Nothing을 사용
        // Result<Nothing>은 모든 Result<T>의 서브타입
    }

    @Test
    fun `Nothing 타입 이해하기`() {
        // Success는 실제 데이터를 가짐
        val success: Result<String> = Result.Success("Hello")
        assertTrue(success is Result.Success)

        // Error는 데이터가 없으므로 Nothing 사용
        val error: Result<String> = Result.Error(Exception("Failed"))
        assertTrue(error is Result.Error)

        // Loading도 데이터가 없으므로 Nothing 사용
        val loading: Result<String> = Result.Loading
        assertTrue(loading is Result.Loading)

        // Nothing의 핵심: 어떤 타입에든 할당 가능
        val stringResult: Result<String> = Result.Error(Exception())
        val intResult: Result<Int> = Result.Error(Exception())
        val userResult: Result<User> = Result.Loading

        // when 표현식에서 활용
        fun process(result: Result<String>): String {
            return when (result) {
                is Result.Success -> "Data: ${result.data}"
                is Result.Error -> "Error: ${result.exception.message}"
                is Result.Loading -> "Loading..."
            }
        }

        assertEquals("Data: Hello", process(success))
        assertEquals("Error: Failed", process(error))
        assertEquals("Loading...", process(loading))
    }

    data class User(val id: Long, val name: String, val email: String)

    /**
     * 예제 5: 실전 Result 패턴
     * out T를 사용하여 타입 안전한 결과 처리
     */
    sealed class ApiResult<out T> {
        data class Success<T>(
            val data: T,
            val timestamp: Long = System.currentTimeMillis()
        ) : ApiResult<T>()

        data class Error(
            val message: String,
            val code: Int? = null,
            val cause: Throwable? = null
        ) : ApiResult<Nothing>()

        data object Loading : ApiResult<Nothing>()
        data object Empty : ApiResult<Nothing>()
    }

    @Test
    fun `실전 Result 패턴 - API 응답`() {
        // 데이터베이스에서 사용자 조회 시뮬레이션
        fun findUser(id: Long): ApiResult<User> {
            return when {
                id > 0 -> ApiResult.Success(User(id, "User $id", "user$id@example.com"))
                id == 0L -> ApiResult.Empty
                else -> ApiResult.Error("Invalid user ID", 400)
            }
        }

        // Success 케이스
        val success = findUser(1)
        when (success) {
            is ApiResult.Success -> {
                assertEquals(1L, success.data.id)
                assertEquals("User 1", success.data.name)
                assertTrue(success.timestamp > 0)
            }
            else -> fail("Should be success")
        }

        // Empty 케이스
        val empty = findUser(0)
        assertTrue(empty is ApiResult.Empty)

        // Error 케이스
        val error = findUser(-1)
        when (error) {
            is ApiResult.Error -> {
                assertEquals("Invalid user ID", error.message)
                assertEquals(400, error.code)
            }
            else -> fail("Should be error")
        }
    }

    /**
     * 예제 6: 제네릭 확장 함수
     * Result에 유용한 확장 함수들
     */
    @Test
    fun `제네릭 확장 함수`() {
        // getOrNull 확장 함수
        fun <T> Result<T>.getOrNull(): T? = when (this) {
            is Result.Success -> data
            else -> null
        }

        // getOrElse 확장 함수
        fun <T> Result<T>.getOrElse(default: T): T = when (this) {
            is Result.Success -> data
            else -> default
        }

        // map 확장 함수
        fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
            is Result.Success -> Result.Success(transform(data))
            is Result.Error -> this
            is Result.Loading -> this
        }

        // flatMap 확장 함수
        fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
            is Result.Success -> transform(data)
            is Result.Error -> this
            is Result.Loading -> this
        }

        val success = Result.Success("hello")
        assertEquals("hello", success.getOrNull())
        assertEquals("hello", success.getOrElse("default"))

        val error = Result.Error(Exception("Failed"))
        assertNull(error.getOrNull())
        assertEquals("default", error.getOrElse("default"))

        // map 체이닝
        val mapped = Result.Success("hello")
            .map { it.uppercase() }
            .map { it.length }

        assertEquals(5, (mapped as Result.Success).data)

        // flatMap 체이닝
        fun validateLength(s: String): Result<String> {
            return if (s.length >= 3) {
                Result.Success(s)
            } else {
                Result.Error(Exception("Too short"))
            }
        }

        val valid = Result.Success("hello").flatMap { validateLength(it) }
        assertTrue(valid is Result.Success)

        val invalid = Result.Success("hi").flatMap { validateLength(it) }
        assertTrue(invalid is Result.Error)
    }

    /**
     * 예제 7: 제네릭 리스트 처리
     * out T가 List에서 어떻게 동작하는지
     */
    @Test
    fun `제네릭 리스트와 공변성`() {
        // List<T>는 out T를 사용
        // interface List<out E> : Collection<E>

        // Number의 리스트
        val numbers: List<Number> = listOf<Number>(1, 2, 3.5, 4.2f)

        // Int는 Number의 서브타입
        val integers: List<Int> = listOf(1, 2, 3)

        // out 덕분에 가능!
        val numbersFromInts: List<Number> = integers

        assertEquals(3, numbersFromInts.size)

        // 함수 파라미터에서 활용
        fun printAll(items: List<Any>) {
            items.forEach { println(it) }
        }

        printAll(listOf("A", "B", "C"))  // List<String> -> List<Any> OK!
        printAll(listOf(1, 2, 3))        // List<Int> -> List<Any> OK!
    }

    /**
     * 예제 8: 타입 경계 (Type Bounds)
     * <T : SuperType> 형태로 타입 제한
     */
    @Test
    fun `타입 경계 - Upper Bound`() {
        // Number 또는 그 서브타입만 허용
        fun <T : Number> double(value: T): Double {
            return value.toDouble() * 2
        }

        assertEquals(20.0, double(10))
        assertEquals(6.0, double(3.0f))
        assertEquals(10.0, double(5L))
        // double("string") // 컴파일 에러!

        // Comparable 구현 타입만 허용
        fun <T : Comparable<T>> max(a: T, b: T): T {
            return if (a > b) a else b
        }

        assertEquals(10, max(5, 10))
        assertEquals("world", max("hello", "world"))
        assertEquals(3.14, max(2.71, 3.14))
    }

    /**
     * 예제 9: 여러 타입 파라미터
     * Map과 같이 키-값 쌍 처리
     */
    @Test
    fun `여러 타입 파라미터 사용`() {
        // 키-값 저장소
        class Storage<K, V> {
            private val map = mutableMapOf<K, V>()

            fun put(key: K, value: V) {
                map[key] = value
            }

            fun get(key: K): V? = map[key]

            fun contains(key: K): Boolean = map.containsKey(key)

            fun size(): Int = map.size
        }

        val userStorage = Storage<Long, User>()
        userStorage.put(1L, User(1, "Alice", "alice@example.com"))
        userStorage.put(2L, User(2, "Bob", "bob@example.com"))

        val alice = userStorage.get(1L)
        assertEquals("Alice", alice?.name)

        assertTrue(userStorage.contains(1L))
        assertFalse(userStorage.contains(999L))
        assertEquals(2, userStorage.size())

        // String을 키로, Int를 값으로 사용
        val scores = Storage<String, Int>()
        scores.put("Alice", 95)
        scores.put("Bob", 87)

        assertEquals(95, scores.get("Alice"))
    }

    /**
     * 예제 10: Reified 타입 파라미터
     * inline 함수에서 실제 타입 정보 접근
     */
    @Test
    fun `reified 타입 파라미터`() {
        // 일반 제네릭은 런타임에 타입 정보 소실 (Type Erasure)
        // reified를 사용하면 런타임에도 타입 정보 유지
        // isInstance와 parseJson은 파일 상단에 정의됨

        assertTrue(isInstance<String>("Hello"))
        assertFalse(isInstance<String>(123))
        assertTrue(isInstance<Int>(123))
        assertFalse(isInstance<Int>("Hello"))

        // JSON 파싱에서 활용
        assertEquals("hello", parseJson<String>("hello"))
        assertEquals(123, parseJson<Int>("123"))
    }

    /**
     * 예제 11: 스타 프로젝션 (Star Projection)
     * 타입을 모를 때 사용하는 * 연산자
     */
    @Test
    fun `스타 프로젝션 이해하기`() {
        // List<*>는 List<out Any?>와 같음
        fun printSize(list: List<*>) {
            println("Size: ${list.size}")
            // val item = list[0] // Any? 타입
        }

        printSize(listOf(1, 2, 3))
        printSize(listOf("A", "B", "C"))

        // Array<*>는 Array<out Any?>
        fun printArray(array: Array<*>) {
            array.forEach { println(it) }
        }

        printArray(arrayOf(1, 2, 3))
        printArray(arrayOf("A", "B", "C"))

        // 실용 예제: 타입을 모르는 Result 처리
        fun isSuccess(result: Result<*>): Boolean {
            return result is Result.Success
        }

        assertTrue(isSuccess(Result.Success("data")))
        assertTrue(isSuccess(Result.Success(123)))
        assertFalse(isSuccess(Result.Error(Exception())))
    }

    /**
     * 예제 12: 실전 Repository 패턴
     * 제네릭을 활용한 재사용 가능한 Repository
     */
    interface Entity {
        val id: Long
    }

    data class Product(
        override val id: Long,
        val name: String,
        val price: BigDecimal
    ) : Entity

    data class Order(
        override val id: Long,
        val productIds: List<Long>,
        val total: BigDecimal
    ) : Entity

    abstract class Repository<T : Entity> {
        protected val storage = mutableMapOf<Long, T>()

        fun save(entity: T): Result<T> {
            storage[entity.id] = entity
            return Result.Success(entity)
        }

        fun findById(id: Long): Result<T> {
            val entity = storage[id]
            return if (entity != null) {
                Result.Success(entity)
            } else {
                Result.Error(Exception("Entity not found: $id"))
            }
        }

        fun findAll(): Result<List<T>> {
            return Result.Success(storage.values.toList())
        }

        fun delete(id: Long): Result<Unit> {
            return if (storage.remove(id) != null) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Entity not found: $id"))
            }
        }
    }

    class ProductRepository : Repository<Product>()
    class OrderRepository : Repository<Order>()

    @Test
    fun `실전 Repository 패턴`() {
        val productRepo = ProductRepository()
        val orderRepo = OrderRepository()

        // Product 저장
        val product = Product(1, "Laptop", BigDecimal("1500000"))
        val savedProduct = productRepo.save(product)
        assertTrue(savedProduct is Result.Success)

        // Product 조회
        val foundProduct = productRepo.findById(1)
        when (foundProduct) {
            is Result.Success -> assertEquals("Laptop", foundProduct.data.name)
            else -> fail("Should be success")
        }

        // Order 저장
        val order = Order(100, listOf(1), BigDecimal("1500000"))
        orderRepo.save(order)

        // 전체 조회
        productRepo.save(Product(2, "Mouse", BigDecimal("50000")))
        val allProducts = productRepo.findAll()
        when (allProducts) {
            is Result.Success -> assertEquals(2, allProducts.data.size)
            else -> fail("Should be success")
        }

        // 삭제
        val deleted = productRepo.delete(1)
        assertTrue(deleted is Result.Success)

        val notFound = productRepo.findById(1)
        assertTrue(notFound is Result.Error)
    }

    /**
     * 예제 13: 제네릭 빌더 패턴
     */
    @Test
    fun `제네릭 빌더 패턴`() {
        class QueryBuilder<T> {
            private var table: String = ""
            private val conditions = mutableListOf<String>()
            private val orderBy = mutableListOf<String>()
            private var limitValue: Int? = null

            fun from(table: String): QueryBuilder<T> {
                this.table = table
                return this
            }

            fun where(condition: String): QueryBuilder<T> {
                conditions.add(condition)
                return this
            }

            fun orderBy(column: String, direction: String = "ASC"): QueryBuilder<T> {
                orderBy.add("$column $direction")
                return this
            }

            fun limit(n: Int): QueryBuilder<T> {
                limitValue = n
                return this
            }

            fun build(): String {
                val sql = StringBuilder("SELECT * FROM $table")

                if (conditions.isNotEmpty()) {
                    sql.append(" WHERE ${conditions.joinToString(" AND ")}")
                }

                if (orderBy.isNotEmpty()) {
                    sql.append(" ORDER BY ${orderBy.joinToString(", ")}")
                }

                limitValue?.let {
                    sql.append(" LIMIT $it")
                }

                return sql.toString()
            }
        }

        val userQuery = QueryBuilder<User>()
            .from("users")
            .where("age >= 18")
            .where("active = true")
            .orderBy("created_at", "DESC")
            .limit(10)
            .build()

        assertTrue(userQuery.contains("SELECT * FROM users"))
        assertTrue(userQuery.contains("WHERE"))
        assertTrue(userQuery.contains("age >= 18"))
        assertTrue(userQuery.contains("ORDER BY"))
        assertTrue(userQuery.contains("LIMIT 10"))
    }
}
