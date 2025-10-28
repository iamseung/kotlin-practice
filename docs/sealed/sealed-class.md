# Sealed Class - 제한된 클래스 계층 구조

## 개요

Sealed Class는 특정 타입의 모든 하위 클래스를 제한하여 타입 안전성을 보장하는 Kotlin의 강력한 기능입니다. 주로 상태, 결과, 이벤트 등을 표현할 때 사용하며, when 표현식과 함께 사용하면 모든 경우를 컴파일 타임에 검증할 수 있습니다.

## 핵심 특징

- **제한된 계층 구조**: 같은 파일(또는 패키지, Kotlin 1.5+)에서만 서브클래스 정의 가능
- **추상 클래스**: 직접 인스턴스화 불가
- **타입 안전성**: when 표현식에서 모든 케이스를 처리하면 else 불필요
- **컴파일 타임 검증**: 새로운 서브클래스 추가 시 컴파일 오류로 누락 방지

## 기본 문법

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

// 사용
fun handle(result: Result<String>) {
    when (result) {
        is Result.Success -> println(result.data)
        is Result.Error -> println(result.exception.message)
        is Result.Loading -> println("Loading...")
        // else 불필요!
    }
}
```

## Sealed Class vs Enum vs Abstract Class

| 특징 | Enum | Sealed Class | Abstract Class |
|------|------|--------------|----------------|
| 인스턴스 | 고정된 상수 | 여러 인스턴스 가능 | 여러 인스턴스 가능 |
| 상태 저장 | 제한적 | 자유롭게 가능 | 자유롭게 가능 |
| 서브클래스 | 불가 | 제한적 허용 | 제한 없음 |
| 타입 안전성 | ✓ | ✓ | ✗ |
| when 완전성 | ✓ | ✓ | ✗ |

```kotlin
// Enum - 고정된 상수
enum class Status { PENDING, SUCCESS, ERROR }

// Sealed Class - 각 상태마다 다른 데이터
sealed class Result {
    data class Success(val data: String) : Result()
    data class Error(val message: String) : Result()
}

// Abstract Class - 제한 없음 (타입 안전성 없음)
abstract class Response
```

## 언제 사용하는가?

### 1. API 응답 처리
```kotlin
sealed class NetworkResponse<out T> {
    data class Success<T>(val data: T, val statusCode: Int) : NetworkResponse<T>()
    data class Error(val statusCode: Int, val message: String) : NetworkResponse<Nothing>()
    data object NetworkError : NetworkResponse<Nothing>()
    data object TimeoutError : NetworkResponse<Nothing>()
}
```

### 2. UI 상태 관리
```kotlin
sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
    data object Empty : UiState<Nothing>()
}
```

### 3. 비즈니스 로직 상태
```kotlin
sealed class PaymentStatus {
    data object Pending : PaymentStatus()
    data class Processing(val transactionId: String) : PaymentStatus()
    data class Success(val transactionId: String, val amount: BigDecimal) : PaymentStatus()
    data class Failed(val reason: String, val retryable: Boolean) : PaymentStatus()
}
```

### 4. 이벤트 시스템
```kotlin
sealed class UserEvent {
    data class Created(val userId: Long, val timestamp: LocalDateTime) : UserEvent()
    data class Updated(val userId: Long, val fields: Map<String, Any>) : UserEvent()
    data class Deleted(val userId: Long) : UserEvent()
}
```

## 실무 예제

### 예제 1: Result 타입 패턴

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

// 사용
fun fetchUser(id: Long): Result<User> {
    return try {
        val user = database.findUser(id)
        Result.Success(user)
    } catch (e: Exception) {
        Result.Error(e)
    }
}

// 처리
when (val result = fetchUser(1)) {
    is Result.Success -> println("User: ${result.data.name}")
    is Result.Error -> println("Error: ${result.exception.message}")
    is Result.Loading -> println("Loading...")
}
```

**장점**:
- 예외 처리를 타입 시스템으로 강제
- null 대신 명시적인 에러 정보
- 로딩 상태도 함께 표현 가능

### 예제 2: UI 상태 관리

```kotlin
sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val code: Int? = null) : UiState<Nothing>()
    data object Empty : UiState<Nothing>()
}

class UserViewModel {
    private val _state = MutableStateFlow<UiState<List<User>>>(UiState.Idle)
    val state: StateFlow<UiState<List<User>>> = _state

    fun loadUsers() {
        _state.value = UiState.Loading

        viewModelScope.launch {
            try {
                val users = repository.getUsers()
                _state.value = if (users.isEmpty()) {
                    UiState.Empty
                } else {
                    UiState.Success(users)
                }
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// UI에서 사용
fun render(state: UiState<List<User>>) {
    when (state) {
        is UiState.Idle -> showWelcomeScreen()
        is UiState.Loading -> showLoadingSpinner()
        is UiState.Success -> showUsers(state.data)
        is UiState.Error -> showError(state.message)
        is UiState.Empty -> showEmptyState()
    }
}
```

### 예제 3: 폼 검증

```kotlin
sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val errors: List<ValidationError>) : ValidationResult()
}

data class ValidationError(val field: String, val message: String)

fun validateUserForm(form: UserForm): ValidationResult {
    val errors = mutableListOf<ValidationError>()

    if (form.username.length < 3) {
        errors.add(ValidationError("username", "최소 3자 이상"))
    }
    if (!form.email.contains("@")) {
        errors.add(ValidationError("email", "유효한 이메일 형식이 아닙니다"))
    }

    return if (errors.isEmpty()) {
        ValidationResult.Valid
    } else {
        ValidationResult.Invalid(errors)
    }
}

// 사용
when (val result = validateUserForm(form)) {
    is ValidationResult.Valid -> submitForm(form)
    is ValidationResult.Invalid -> {
        result.errors.forEach { error ->
            showFieldError(error.field, error.message)
        }
    }
}
```

### 예제 4: 명령 패턴 (Command Pattern)

```kotlin
sealed class OrderCommand {
    data class Create(
        val userId: Long,
        val items: List<String>,
        val total: BigDecimal
    ) : OrderCommand()

    data class Update(
        val orderId: Long,
        val items: List<String>
    ) : OrderCommand()

    data class Cancel(
        val orderId: Long,
        val reason: String
    ) : OrderCommand()

    data class Ship(
        val orderId: Long,
        val trackingNumber: String
    ) : OrderCommand()
}

class OrderService {
    fun execute(command: OrderCommand): Result<Order> {
        return when (command) {
            is OrderCommand.Create -> createOrder(command)
            is OrderCommand.Update -> updateOrder(command)
            is OrderCommand.Cancel -> cancelOrder(command)
            is OrderCommand.Ship -> shipOrder(command)
        }
    }

    private fun createOrder(cmd: OrderCommand.Create): Result<Order> {
        // 주문 생성 로직
        val order = Order(
            userId = cmd.userId,
            items = cmd.items,
            total = cmd.total
        )
        return Result.Success(order)
    }

    // 다른 메서드들...
}
```

### 예제 5: Either 타입 (함수형 프로그래밍)

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

    fun <T> map(transform: (R) -> T): Either<L, T> = when (this) {
        is Left -> this
        is Right -> Right(transform(value))
    }

    fun isLeft(): Boolean = this is Left
    fun isRight(): Boolean = this is Right

    fun getOrNull(): R? = when (this) {
        is Left -> null
        is Right -> value
    }
}

// 사용 예제
fun divide(a: Int, b: Int): Either<String, Int> {
    return if (b == 0) {
        Either.Left("0으로 나눌 수 없습니다")
    } else {
        Either.Right(a / b)
    }
}

val result = divide(10, 2)
    .map { it * 2 }  // Right(10)
    .fold(
        onLeft = { "Error: $it" },
        onRight = { "Result: $it" }
    )
// "Result: 10"
```

### 예제 6: 재귀적 Sealed Class (트리 구조)

```kotlin
sealed class JsonValue {
    data class JsonObject(val values: Map<String, JsonValue>) : JsonValue()
    data class JsonArray(val items: List<JsonValue>) : JsonValue()
    data class JsonString(val value: String) : JsonValue()
    data class JsonNumber(val value: Double) : JsonValue()
    data class JsonBoolean(val value: Boolean) : JsonValue()
    data object JsonNull : JsonValue()
}

// JSON 생성
val json = JsonValue.JsonObject(
    mapOf(
        "name" to JsonValue.JsonString("John"),
        "age" to JsonValue.JsonNumber(30.0),
        "active" to JsonValue.JsonBoolean(true),
        "tags" to JsonValue.JsonArray(
            listOf(
                JsonValue.JsonString("kotlin"),
                JsonValue.JsonString("developer")
            )
        ),
        "spouse" to JsonValue.JsonNull
    )
)

// JSON 파싱
fun parseJson(value: JsonValue): String {
    return when (value) {
        is JsonValue.JsonObject -> {
            value.values.entries.joinToString(", ", "{", "}") { (k, v) ->
                "\"$k\": ${parseJson(v)}"
            }
        }
        is JsonValue.JsonArray -> {
            value.items.joinToString(", ", "[", "]") { parseJson(it) }
        }
        is JsonValue.JsonString -> "\"${value.value}\""
        is JsonValue.JsonNumber -> value.value.toString()
        is JsonValue.JsonBoolean -> value.value.toString()
        is JsonValue.JsonNull -> "null"
    }
}
```

### 예제 7: 페이지네이션 상태

```kotlin
sealed class PaginationState<out T> {
    data object Initial : PaginationState<Nothing>()
    data class Loading(val isFirstPage: Boolean) : PaginationState<Nothing>()
    data class Success<T>(
        val items: List<T>,
        val currentPage: Int,
        val totalPages: Int,
        val hasMore: Boolean
    ) : PaginationState<T>()
    data class Error(val message: String, val canRetry: Boolean) : PaginationState<Nothing>()
    data class LoadingMore<T>(val currentItems: List<T>) : PaginationState<T>()
}

class PaginatedListViewModel {
    private val _state = MutableStateFlow<PaginationState<Item>>(PaginationState.Initial)

    fun loadFirstPage() {
        _state.value = PaginationState.Loading(isFirstPage = true)
        // 로딩 로직...
    }

    fun loadMore() {
        val current = _state.value
        if (current is PaginationState.Success && current.hasMore) {
            _state.value = PaginationState.LoadingMore(current.items)
            // 추가 로딩 로직...
        }
    }
}
```

### 예제 8: 인증 상태

```kotlin
sealed class AuthState {
    data object Unauthenticated : AuthState()

    data class Authenticated(
        val userId: Long,
        val username: String,
        val token: String,
        val roles: Set<String>
    ) : AuthState()

    data class SessionExpired(
        val lastUserId: Long
    ) : AuthState()

    data class RequiresMfa(
        val userId: Long,
        val method: MfaMethod
    ) : AuthState()
}

enum class MfaMethod { SMS, EMAIL, TOTP }

fun checkAccess(state: AuthState, requiredRole: String): Boolean {
    return when (state) {
        is AuthState.Authenticated -> requiredRole in state.roles
        is AuthState.Unauthenticated,
        is AuthState.SessionExpired,
        is AuthState.RequiresMfa -> false
    }
}
```

## 패턴 및 베스트 프랙티스

### 1. Result 패턴과 확장 함수

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

// 편리한 확장 함수
fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    is Result.Error -> null
}

fun <T> Result<T>.getOrElse(default: T): T = when (this) {
    is Result.Success -> data
    is Result.Error -> default
}

fun <T> Result<T>.getOrThrow(): T = when (this) {
    is Result.Success -> data
    is Result.Error -> throw exception
}

fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
}

fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
    is Result.Success -> transform(data)
    is Result.Error -> this
}

// 사용
val result = fetchUser(1)
    .map { it.email }
    .map { it.lowercase() }
    .getOrNull()
```

### 2. 상태 전이 관리

```kotlin
sealed class OrderState {
    data object Created : OrderState()
    data object PaymentPending : OrderState()
    data object Paid : OrderState()
    data object Shipped : OrderState()
    data object Delivered : OrderState()
    data object Cancelled : OrderState()

    fun canTransitionTo(newState: OrderState): Boolean {
        return when (this) {
            is Created -> newState is PaymentPending || newState is Cancelled
            is PaymentPending -> newState is Paid || newState is Cancelled
            is Paid -> newState is Shipped || newState is Cancelled
            is Shipped -> newState is Delivered
            is Delivered -> false
            is Cancelled -> false
        }
    }
}

class Order(private var state: OrderState) {
    fun transitionTo(newState: OrderState) {
        if (!state.canTransitionTo(newState)) {
            throw IllegalStateException(
                "Cannot transition from $state to $newState"
            )
        }
        state = newState
    }
}
```

### 3. Smart Cast 활용

```kotlin
sealed class Shape {
    data class Circle(val radius: Double) : Shape()
    data class Rectangle(val width: Double, val height: Double) : Shape()
    data class Triangle(val base: Double, val height: Double) : Shape()
}

fun calculateArea(shape: Shape): Double {
    return when (shape) {
        is Shape.Circle -> {
            // shape는 자동으로 Circle로 smart cast
            Math.PI * shape.radius * shape.radius
        }
        is Shape.Rectangle -> {
            // shape는 자동으로 Rectangle로 smart cast
            shape.width * shape.height
        }
        is Shape.Triangle -> {
            // shape는 자동으로 Triangle로 smart cast
            0.5 * shape.base * shape.height
        }
    }
}
```

## 주의사항

### ❌ 안티패턴

1. **너무 많은 서브클래스**
```kotlin
// Bad - 관리하기 어려움
sealed class AppState {
    data object State1 : AppState()
    data object State2 : AppState()
    // ... 20개 이상의 서브클래스
}

// Good - 계층적으로 구성
sealed class AppState {
    sealed class Loading : AppState() {
        data object Initial : Loading()
        data object Refreshing : Loading()
    }
    sealed class Content : AppState() {
        data class Data(val items: List<Item>) : Content()
        data object Empty : Content()
    }
    data class Error(val message: String) : AppState()
}
```

2. **Enum으로 충분한 경우**
```kotlin
// Bad - Sealed class 불필요
sealed class Color {
    data object Red : Color()
    data object Green : Color()
    data object Blue : Color()
}

// Good - Enum 사용
enum class Color { RED, GREEN, BLUE }
```

3. **상태 없는 서브클래스에 class 사용**
```kotlin
// Bad
sealed class Result {
    class Loading : Result()  // 매번 새 인스턴스
}

// Good
sealed class Result {
    data object Loading : Result()  // 싱글톤
}
```

## 요약

Sealed Class는 제한된 타입 계층 구조를 만들어 타입 안전성을 보장하는 강력한 도구입니다:

**장점**:
- ✅ 컴파일 타임 타입 안전성
- ✅ when 표현식에서 완전성 검사
- ✅ 새로운 케이스 추가 시 컴파일 오류로 누락 방지
- ✅ 각 서브클래스마다 다른 데이터 저장 가능
- ✅ 상태, 결과, 이벤트를 명확하게 표현

**사용 시기**:
- API 응답 처리 (Success, Error, Loading)
- UI 상태 관리
- 비즈니스 로직의 상태 표현
- 도메인 이벤트
- Result/Either 타입
- 명령 패턴

**vs Enum**: 각 케이스마다 다른 데이터가 필요하면 Sealed Class, 고정된 상수면 Enum
**vs Abstract Class**: 제한된 계층 구조가 필요하면 Sealed Class, 제한 없으면 Abstract Class

## 관련 문서

- [Kotlin 공식 문서 - Sealed Classes](https://kotlinlang.org/docs/sealed-classes.html)
- [when 표현식](https://kotlinlang.org/docs/control-flow.html#when-expression)
- 테스트 코드: `src/test/kotlin/com/example/kotlin/single_test/SealedClassTest.kt`
