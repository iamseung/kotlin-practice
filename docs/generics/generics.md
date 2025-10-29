# Kotlin Generics (제네릭) 완벽 가이드

## 개요

제네릭은 타입을 파라미터화하여 코드의 재사용성과 타입 안전성을 동시에 확보하는 Kotlin의 핵심 기능입니다. 특히 Sealed Class와 함께 사용될 때, `out T`와 `Nothing` 타입의 이해가 매우 중요합니다.

## 핵심 개념

### 1. 제네릭 타입 파라미터

```kotlin
// 기본 형태
class Box<T>(val value: T)

// 여러 타입 파라미터
class Pair<K, V>(val key: K, val value: V)

// 함수에서 사용
fun <T> identity(value: T): T = value
```

### 2. Variance (변성)

Kotlin의 제네릭에서 가장 중요한 개념입니다.

| 키워드 | 이름 | 의미 | 사용 위치 | 예시 |
|--------|------|------|-----------|------|
| `out T` | 공변성 (Covariance) | 생산자, 읽기 전용 | 반환 타입 | `List<out T>` |
| `in T` | 반공변성 (Contravariance) | 소비자, 쓰기 전용 | 파라미터 타입 | `Comparator<in T>` |
| `T` | 불변성 (Invariance) | 읽기/쓰기 모두 | 양방향 | `MutableList<T>` |

### 3. Nothing 타입

- 모든 타입의 서브타입 (bottom type)
- 실제 값이 없음을 나타냄
- 주로 에러나 로딩 상태 등 데이터가 없는 경우에 사용

## sealed class에서 제네릭 활용

### 기본 패턴 분석

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}
```

**왜 이렇게 설계했을까?**

#### 1. `out T` 사용 이유

```kotlin
// out T 덕분에 가능한 일
val stringResult: Result<String> = Result.Success("Hello")
val anyResult: Result<Any> = stringResult  // OK! 공변성

// out이 없다면?
// sealed class Result<T>  // 불변성
// val anyResult: Result<Any> = stringResult  // 컴파일 에러!
```

**out T의 핵심**:
- Result는 T를 "생산"만 합니다 (반환만 함)
- T를 파라미터로 받지 않습니다 (쓰기 없음)
- 부모 타입을 자식 타입에 할당 가능

#### 2. `Nothing` 사용 이유

```kotlin
// Error와 Loading은 실제 데이터가 없음
data class Error(val exception: Exception) : Result<Nothing>()
data object Loading : Result<Nothing>()

// Nothing 덕분에 가능한 일
val stringResult: Result<String> = Result.Error(Exception())  // OK!
val intResult: Result<Int> = Result.Error(Exception())        // OK!
val userResult: Result<User> = Result.Loading                 // OK!

// Nothing은 모든 타입의 서브타입이므로
// Result<Nothing>은 Result<Any>의 서브타입
```

**Nothing의 핵심**:
- 값이 없음을 명시적으로 표현
- 모든 타입에 할당 가능
- Error나 Loading처럼 데이터 없는 상태에 완벽

### 실전 예제: API 응답 처리

```kotlin
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

// 사용 예제
fun fetchUser(id: Long): ApiResult<User> {
    return try {
        val user = database.findUser(id)
        ApiResult.Success(user)
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Unknown error", cause = e)
    }
}

// 처리
when (val result = fetchUser(1)) {
    is ApiResult.Success -> {
        // result.data는 User 타입
        println("User: ${result.data.name}")
    }
    is ApiResult.Error -> {
        println("Error: ${result.message}")
    }
    is ApiResult.Loading -> {
        println("Loading...")
    }
    is ApiResult.Empty -> {
        println("No data")
    }
}
```

## out T (공변성) 상세 설명

### 개념

**"이 타입은 T를 생산(produce)만 합니다"**

```kotlin
interface Producer<out T> {
    fun produce(): T           // OK! T를 반환
    // fun consume(item: T)    // 컴파일 에러! T를 파라미터로 사용 불가
}
```

### 왜 out이라고 부를까?

- T가 "나가는(out)" 방향으로만 사용
- 메서드의 반환 타입에만 사용 가능
- 파라미터 타입으로는 사용 불가

### 실용 예제

```kotlin
// List<out E>는 읽기 전용
val strings: List<String> = listOf("A", "B", "C")
val anys: List<Any> = strings  // OK! List는 out E를 사용

// 읽기만 가능
val first: Any = anys[0]  // OK
// anys.add("D")  // 컴파일 에러! List는 불변

// MutableList<E>는 불변성 (out 없음)
val mutableStrings: MutableList<String> = mutableListOf("A", "B")
// val mutableAnys: MutableList<Any> = mutableStrings  // 컴파일 에러!
```

### 계층 구조 이해

```kotlin
// String은 Any의 서브타입
// Producer<String>은 Producer<Any>의 서브타입 (공변성)

class StringProducer : Producer<String> {
    override fun produce(): String = "Hello"
}

val stringProducer: Producer<String> = StringProducer()
val anyProducer: Producer<Any> = stringProducer  // OK!
```

## in T (반공변성) 상세 설명

### 개념

**"이 타입은 T를 소비(consume)만 합니다"**

```kotlin
interface Consumer<in T> {
    fun consume(item: T)       // OK! T를 파라미터로 받음
    // fun produce(): T        // 컴파일 에러! T를 반환 타입으로 사용 불가
}
```

### 왜 in이라고 부를까?

- T가 "들어오는(in)" 방향으로만 사용
- 메서드의 파라미터 타입에만 사용 가능
- 반환 타입으로는 사용 불가 (Any?만 가능)

### 실용 예제

```kotlin
// Comparator<in T>
interface Comparator<in T> {
    fun compare(a: T, b: T): Int
}

// Any는 String의 슈퍼타입
// Comparator<Any>는 Comparator<String>의 서브타입 (반공변성)

val anyComparator: Comparator<Any> = object : Comparator<Any> {
    override fun compare(a: Any, b: Any): Int {
        return a.toString().compareTo(b.toString())
    }
}

val stringComparator: Comparator<String> = anyComparator  // OK!
```

## Nothing 타입 완벽 이해

### Nothing이란?

- Kotlin의 특별한 타입
- 모든 타입의 서브타입 (bottom type)
- 인스턴스가 존재하지 않음
- "값이 없음"을 타입 레벨에서 표현

### 왜 필요한가?

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<????>()  // 여기에 뭘 넣어야 할까?
}
```

**선택지**:

1. **`Result<Any?>`** - 나쁨
   ```kotlin
   data class Error(val message: String) : Result<Any?>()

   // 문제: Any?는 String의 슈퍼타입이 아님
   val stringResult: Result<String> = Result.Error("Failed")  // 타입 불일치!
   ```

2. **타입 파라미터 유지** - 불필요
   ```kotlin
   data class Error<T>(val message: String) : Result<T>()

   // 문제: T를 전혀 사용하지 않는데 명시해야 함
   val error: Result<String> = Result.Error<String>("Failed")  // 번거로움
   ```

3. **`Nothing` 사용** - 완벽! ✓
   ```kotlin
   data class Error(val message: String) : Result<Nothing>()

   // Nothing은 모든 타입의 서브타입
   val stringResult: Result<String> = Result.Error("Failed")  // OK!
   val intResult: Result<Int> = Result.Error("Failed")        // OK!
   ```

### Nothing의 동작 원리

```kotlin
// 타입 계층 구조
Any
 ├─ String
 ├─ Int
 ├─ User
 └─ ...
    └─ Nothing  // 모든 타입의 서브타입

// 공변성(out)과 결합
Result<Nothing> <: Result<String> <: Result<Any>

// 따라서 가능:
val stringResult: Result<String> = Result<Nothing>.Error("...")
val anyResult: Result<Any> = Result<Nothing>.Error("...")
```

### 실전 활용

```kotlin
// UI 상태 관리
sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

// 어떤 타입에든 할당 가능
val userState: UiState<User> = UiState.Loading       // OK!
val listState: UiState<List<Item>> = UiState.Error("Failed")  // OK!
```

## 제네릭 확장 함수

Result 타입을 더 편리하게 사용하기 위한 확장 함수들:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

// 1. getOrNull - 성공 시 데이터, 실패 시 null
fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}

// 2. getOrElse - 성공 시 데이터, 실패 시 기본값
fun <T> Result<T>.getOrElse(default: T): T = when (this) {
    is Result.Success -> data
    else -> default
}

// 3. getOrThrow - 성공 시 데이터, 실패 시 예외
fun <T> Result<T>.getOrThrow(): T = when (this) {
    is Result.Success -> data
    is Result.Error -> throw exception
    is Result.Loading -> throw IllegalStateException("Still loading")
}

// 4. map - 성공 시 데이터 변환
fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}

// 5. flatMap - 성공 시 새로운 Result 반환
fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
    is Result.Success -> transform(data)
    is Result.Error -> this
    is Result.Loading -> this
}

// 6. fold - 모든 경우를 처리
fun <T, R> Result<T>.fold(
    onSuccess: (T) -> R,
    onError: (Exception) -> R,
    onLoading: () -> R
): R = when (this) {
    is Result.Success -> onSuccess(data)
    is Result.Error -> onError(exception)
    is Result.Loading -> onLoading()
}

// 사용 예제
val result = Result.Success("hello")
    .map { it.uppercase() }      // Result.Success("HELLO")
    .map { it.length }            // Result.Success(5)
    .getOrElse(0)                 // 5
```

## 타입 경계 (Type Bounds)

특정 타입만 허용하도록 제한:

```kotlin
// Upper Bound: Number 또는 그 서브타입만
fun <T : Number> double(value: T): Double {
    return value.toDouble() * 2
}

double(10)      // OK
double(3.14)    // OK
double("text")  // 컴파일 에러!

// Multiple Bounds: 여러 제약 조건
fun <T> sort(items: List<T>): List<T>
    where T : Comparable<T>, T : Serializable {
    return items.sorted()
}
```

## 실전 패턴

### 패턴 1: Repository with Result

```kotlin
interface Repository<T : Entity> {
    suspend fun findById(id: Long): Result<T>
    suspend fun findAll(): Result<List<T>>
    suspend fun save(entity: T): Result<T>
    suspend fun delete(id: Long): Result<Unit>
}

class UserRepository : Repository<User> {
    override suspend fun findById(id: Long): Result<User> {
        return try {
            val user = database.find(id)
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

### 패턴 2: 중첩된 제네릭

```kotlin
// Result<List<User>>
val users: Result<List<User>> = fetchUsers()

when (users) {
    is Result.Success -> {
        // users.data는 List<User>
        users.data.forEach { user ->
            println(user.name)
        }
    }
    is Result.Error -> {
        println(users.exception.message)
    }
    is Result.Loading -> {
        println("Loading...")
    }
}
```

### 패턴 3: Either 타입

```kotlin
sealed class Either<out L, out R> {
    data class Left<L>(val value: L) : Either<L, Nothing>()
    data class Right<R>(val value: R) : Either<Nothing, R>()

    fun <T> fold(
        onLeft: (L) -> T,
        onRight: (R) -> T
    ): T = when (this) {
        is Left -> onLeft(value)
        is Right -> onRight(value)
    }
}

// 사용: 에러 타입을 명시적으로
fun divide(a: Int, b: Int): Either<String, Int> {
    return if (b == 0) {
        Either.Left("Cannot divide by zero")
    } else {
        Either.Right(a / b)
    }
}
```

## 흔한 실수와 해결

### 실수 1: out을 빼먹음

```kotlin
// ❌ Bad
sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

val stringResult: Result<String> = Result.Success("Hello")
val anyResult: Result<Any> = stringResult  // 컴파일 에러!

// ✅ Good
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}
```

### 실수 2: Nothing 대신 Any 사용

```kotlin
// ❌ Bad
data class Error(val message: String) : Result<Any>()

val stringResult: Result<String> = Result.Error("Failed")  // 타입 불일치!

// ✅ Good
data class Error(val message: String) : Result<Nothing>()

val stringResult: Result<String> = Result.Error("Failed")  // OK!
```

### 실수 3: 타입 파라미터를 불필요하게 유지

```kotlin
// ❌ Bad - Error에 T가 필요 없음
data class Error<T>(val message: String) : Result<T>()

val error = Result.Error<String>("Failed")  // 번거로움

// ✅ Good
data class Error(val message: String) : Result<Nothing>()

val error = Result.Error("Failed")  // 간결함
```

## 스타 프로젝션 (Star Projection)

타입을 모를 때 사용하는 `*`:

```kotlin
// List<*>는 List<out Any?>와 동일
fun printSize(list: List<*>) {
    println(list.size)  // OK
    // val item = list[0]  // Any? 타입
}

// Result<*>로 타입 무관 처리
fun isSuccess(result: Result<*>): Boolean {
    return result is Result.Success
}

isSuccess(Result.Success("data"))  // true
isSuccess(Result.Success(123))     // true
isSuccess(Result.Error(Exception()))  // false
```

## 요약

### 핵심 개념 정리

1. **`out T` (공변성)**
   - T를 "생산"만 함 (반환만)
   - 부모 타입 → 자식 타입 할당 가능
   - 예: `List<out E>`, `Result<out T>`

2. **`in T` (반공변성)**
   - T를 "소비"만 함 (파라미터만)
   - 자식 타입 → 부모 타입 할당 가능
   - 예: `Comparator<in T>`

3. **`Nothing`**
   - 모든 타입의 서브타입
   - 값이 없음을 표현
   - Error, Loading 등에 사용

### Sealed Class + Generics 체크리스트

- ✅ `sealed class Result<out T>` - out 사용
- ✅ `data class Success<T>(val data: T)` - 실제 데이터
- ✅ `data class Error(...) : Result<Nothing>()` - Nothing 사용
- ✅ `data object Loading : Result<Nothing>()` - Nothing 사용
- ✅ 확장 함수로 편의 기능 제공

### 언제 사용하는가?

- API 응답 처리
- UI 상태 관리
- 비동기 작업 결과
- 타입 안전한 에러 처리
- 재사용 가능한 컴포넌트

## 관련 문서

- [sealed-class.md](./sealed-class.md) - Sealed Class 가이드
- [Kotlin 공식 문서 - Generics](https://kotlinlang.org/docs/generics.html)
- 테스트 코드: `src/test/kotlin/com/example/kotlin/single_test/GenericsTest.kt`
