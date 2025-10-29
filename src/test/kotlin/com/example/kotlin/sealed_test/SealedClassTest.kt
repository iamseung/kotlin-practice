package com.example.kotlin.sealed_test

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Sealed Class - 제한된 클래스 계층 구조
 *
 * 언제 사용하는가?
 * 1. 특정 타입의 모든 하위 클래스를 제한하고 싶을 때
 * 2. when 표현식에서 모든 경우를 처리하도록 강제할 때
 * 3. 상태, 결과, 이벤트 등을 타입 안전하게 표현할 때
 * 4. enum보다 복잡한 데이터를 가진 계층 구조가 필요할 때
 *
 * 특징:
 * - 같은 파일 내에서만 서브클래스 정의 가능 (Kotlin 1.5+: 같은 패키지)
 * - 추상 클래스처럼 직접 인스턴스화 불가
 * - when 표현식에서 모든 케이스를 처리하면 else 불필요
 * - 타입 안전성과 코드 가독성 향상
 */
class SealedClassTest {

    /**
     * 예제 1: 기본 sealed class - Result 타입
     * 성공/실패를 타입 안전하게 표현
     */
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val exception: Exception) : Result<Nothing>()
        data object Loading : Result<Nothing>()
    }

    @Test
    fun `sealed class - 기본 Result 타입`() {
        fun fetchUser(id: Long): Result<String> {
            return when {
                id > 0 -> Result.Success("User $id")
                else -> Result.Error(IllegalArgumentException("Invalid ID"))
            }
        }

        // Success 케이스
        val success = fetchUser(1)
        assertTrue(success is Result.Success)
        when (success) {
            is Result.Success -> assertEquals("User 1", success.data)
            is Result.Error -> fail("Should be success")
            is Result.Loading -> fail("Should be success")
        }

        // Error 케이스
        val error = fetchUser(-1)
        assertTrue(error is Result.Error)
        when (error) {
            is Result.Success -> fail("Should be error")
            is Result.Error -> assertTrue(error.exception is IllegalArgumentException)
            is Result.Loading -> fail("Should be error")
        }

        // Loading 케이스
        val loading = Result.Loading
        assertTrue(loading is Result.Loading)
    }

    /**
     * 예제 2: UI 상태 관리
     * 화면의 다양한 상태를 명확하게 표현
     */
    sealed class UiState<out T> {
        data object Idle : UiState<Nothing>()
        data object Loading : UiState<Nothing>()
        data class Success<T>(val data: T) : UiState<T>()
        data class Error(val message: String, val code: Int? = null) : UiState<Nothing>()
        data object Empty : UiState<Nothing>()
    }

    data class User(val id: Long, val name: String, val email: String)

    @Test
    fun `sealed class - UI 상태 관리`() {
        fun renderUi(state: UiState<List<User>>): String {
            return when (state) {
                is UiState.Idle -> "화면 준비 중..."
                is UiState.Loading -> "데이터 로딩 중..."
                is UiState.Success -> "사용자 ${state.data.size}명 표시"
                is UiState.Error -> "오류 발생: ${state.message}"
                is UiState.Empty -> "데이터가 없습니다"
            }
        }

        val idle = UiState.Idle
        assertEquals("화면 준비 중...", renderUi(idle))

        val loading = UiState.Loading
        assertEquals("데이터 로딩 중...", renderUi(loading))

        val users = listOf(
            User(1, "Alice", "alice@example.com"),
            User(2, "Bob", "bob@example.com")
        )
        val success = UiState.Success(users)
        assertEquals("사용자 2명 표시", renderUi(success))

        val error = UiState.Error("Network timeout", 408)
        assertEquals("오류 발생: Network timeout", renderUi(error))

        val empty = UiState.Empty
        assertEquals("데이터가 없습니다", renderUi(empty))
    }

    /**
     * 예제 3: 네트워크 응답
     * API 응답의 다양한 상태를 처리
     */
    sealed class NetworkResponse<out T> {
        data class Success<T>(
            val data: T,
            val statusCode: Int = 200
        ) : NetworkResponse<T>()

        data class Error(
            val statusCode: Int,
            val message: String,
            val body: String? = null
        ) : NetworkResponse<Nothing>()

        data object NetworkError : NetworkResponse<Nothing>()
        data object TimeoutError : NetworkResponse<Nothing>()
    }

    @Test
    fun `sealed class - 네트워크 응답 처리`() {
        fun handleResponse(response: NetworkResponse<User>): String {
            return when (response) {
                is NetworkResponse.Success -> {
                    "성공: ${response.data.name} (${response.statusCode})"
                }
                is NetworkResponse.Error -> {
                    "HTTP 오류 ${response.statusCode}: ${response.message}"
                }
                is NetworkResponse.NetworkError -> {
                    "네트워크 연결 실패"
                }
                is NetworkResponse.TimeoutError -> {
                    "요청 시간 초과"
                }
            }
        }

        val success = NetworkResponse.Success(
            User(1, "John", "john@example.com"),
            200
        )
        assertEquals("성공: John (200)", handleResponse(success))

        val error404 = NetworkResponse.Error(404, "Not Found")
        assertEquals("HTTP 오류 404: Not Found", handleResponse(error404))

        val networkError = NetworkResponse.NetworkError
        assertEquals("네트워크 연결 실패", handleResponse(networkError))

        val timeout = NetworkResponse.TimeoutError
        assertEquals("요청 시간 초과", handleResponse(timeout))
    }

    /**
     * 예제 4: 결제 상태
     * 복잡한 비즈니스 로직의 상태 표현
     */
    sealed class PaymentStatus {
        data object Pending : PaymentStatus()

        data class Processing(
            val transactionId: String,
            val startedAt: LocalDateTime
        ) : PaymentStatus()

        data class Success(
            val transactionId: String,
            val amount: BigDecimal,
            val completedAt: LocalDateTime
        ) : PaymentStatus()

        data class Failed(
            val transactionId: String?,
            val reason: String,
            val retryable: Boolean
        ) : PaymentStatus()

        data class Cancelled(
            val transactionId: String,
            val cancelledAt: LocalDateTime,
            val reason: String
        ) : PaymentStatus()
    }

    @Test
    fun `sealed class - 결제 상태 관리`() {
        fun processPayment(status: PaymentStatus): String {
            return when (status) {
                is PaymentStatus.Pending -> {
                    "결제 대기 중"
                }
                is PaymentStatus.Processing -> {
                    "결제 진행 중 (거래 ID: ${status.transactionId})"
                }
                is PaymentStatus.Success -> {
                    "결제 완료 - ${status.amount}원 (거래 ID: ${status.transactionId})"
                }
                is PaymentStatus.Failed -> {
                    if (status.retryable) {
                        "결제 실패 (재시도 가능): ${status.reason}"
                    } else {
                        "결제 실패 (재시도 불가): ${status.reason}"
                    }
                }
                is PaymentStatus.Cancelled -> {
                    "결제 취소됨: ${status.reason}"
                }
            }
        }

        val pending = PaymentStatus.Pending
        assertEquals("결제 대기 중", processPayment(pending))

        val processing = PaymentStatus.Processing("TXN-001", LocalDateTime.now())
        assertEquals("결제 진행 중 (거래 ID: TXN-001)", processPayment(processing))

        val success = PaymentStatus.Success(
            "TXN-001",
            BigDecimal("50000"),
            LocalDateTime.now()
        )
        assertTrue(processPayment(success).contains("결제 완료"))

        val failedRetryable = PaymentStatus.Failed("TXN-001", "카드 한도 초과", true)
        assertTrue(processPayment(failedRetryable).contains("재시도 가능"))

        val failedNonRetryable = PaymentStatus.Failed("TXN-001", "도난 카드", false)
        assertTrue(processPayment(failedNonRetryable).contains("재시도 불가"))

        val cancelled = PaymentStatus.Cancelled(
            "TXN-001",
            LocalDateTime.now(),
            "사용자 요청"
        )
        assertTrue(processPayment(cancelled).contains("결제 취소됨"))
    }

    /**
     * 예제 5: 폼 검증 결과
     * 여러 필드의 검증 상태를 표현
     */
    sealed class ValidationResult {
        data object Valid : ValidationResult()

        data class Invalid(
            val errors: List<ValidationError>
        ) : ValidationResult()
    }

    data class ValidationError(
        val field: String,
        val message: String
    )

    @Test
    fun `sealed class - 폼 검증`() {
        data class UserForm(
            val username: String,
            val email: String,
            val password: String
        )

        fun validateForm(form: UserForm): ValidationResult {
            val errors = mutableListOf<ValidationError>()

            if (form.username.length < 3) {
                errors.add(ValidationError("username", "사용자명은 3자 이상이어야 합니다"))
            }

            if (!form.email.contains("@")) {
                errors.add(ValidationError("email", "올바른 이메일 형식이 아닙니다"))
            }

            if (form.password.length < 8) {
                errors.add(ValidationError("password", "비밀번호는 8자 이상이어야 합니다"))
            }

            return if (errors.isEmpty()) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(errors)
            }
        }

        // 유효한 폼
        val validForm = UserForm("john", "john@example.com", "password123")
        val validResult = validateForm(validForm)
        assertTrue(validResult is ValidationResult.Valid)

        // 유효하지 않은 폼
        val invalidForm = UserForm("ab", "invalid-email", "1234")
        val invalidResult = validateForm(invalidForm)
        assertTrue(invalidResult is ValidationResult.Invalid)

        when (invalidResult) {
            is ValidationResult.Invalid -> {
                assertEquals(3, invalidResult.errors.size)
                assertTrue(invalidResult.errors.any { it.field == "username" })
                assertTrue(invalidResult.errors.any { it.field == "email" })
                assertTrue(invalidResult.errors.any { it.field == "password" })
            }
            is ValidationResult.Valid -> fail("Should be invalid")
        }
    }

    /**
     * 예제 6: 이벤트 시스템
     * 다양한 도메인 이벤트를 타입 안전하게 표현
     */
    sealed class UserEvent {
        data class Created(
            val userId: Long,
            val username: String,
            val timestamp: LocalDateTime
        ) : UserEvent()

        data class Updated(
            val userId: Long,
            val changedFields: Map<String, Any>,
            val timestamp: LocalDateTime
        ) : UserEvent()

        data class Deleted(
            val userId: Long,
            val timestamp: LocalDateTime
        ) : UserEvent()

        data class LoggedIn(
            val userId: Long,
            val ip: String,
            val timestamp: LocalDateTime
        ) : UserEvent()

        data class LoggedOut(
            val userId: Long,
            val timestamp: LocalDateTime
        ) : UserEvent()
    }

    @Test
    fun `sealed class - 이벤트 시스템`() {
        val events = mutableListOf<UserEvent>()

        fun handleEvent(event: UserEvent): String {
            events.add(event)

            return when (event) {
                is UserEvent.Created -> {
                    "사용자 생성: ${event.username} (ID: ${event.userId})"
                }
                is UserEvent.Updated -> {
                    "사용자 정보 수정: ${event.changedFields.keys.joinToString()}"
                }
                is UserEvent.Deleted -> {
                    "사용자 삭제: ID ${event.userId}"
                }
                is UserEvent.LoggedIn -> {
                    "로그인: ID ${event.userId}, IP ${event.ip}"
                }
                is UserEvent.LoggedOut -> {
                    "로그아웃: ID ${event.userId}"
                }
            }
        }

        val created = UserEvent.Created(1, "john", LocalDateTime.now())
        assertEquals("사용자 생성: john (ID: 1)", handleEvent(created))

        val updated = UserEvent.Updated(
            1,
            mapOf("email" to "new@example.com"),
            LocalDateTime.now()
        )
        assertTrue(handleEvent(updated).contains("email"))

        val loggedIn = UserEvent.LoggedIn(1, "192.168.1.1", LocalDateTime.now())
        assertTrue(handleEvent(loggedIn).contains("192.168.1.1"))

        val loggedOut = UserEvent.LoggedOut(1, LocalDateTime.now())
        assertTrue(handleEvent(loggedOut).contains("로그아웃"))

        val deleted = UserEvent.Deleted(1, LocalDateTime.now())
        assertTrue(handleEvent(deleted).contains("삭제"))

        assertEquals(5, events.size)
    }

    /**
     * 예제 7: 명령 패턴 (Command Pattern)
     * 다양한 명령을 타입 안전하게 표현
     */
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

        data class Complete(
            val orderId: Long
        ) : OrderCommand()
    }

    @Test
    fun `sealed class - 명령 패턴`() {
        fun executeCommand(command: OrderCommand): String {
            return when (command) {
                is OrderCommand.Create -> {
                    "주문 생성: 사용자 ${command.userId}, 상품 ${command.items.size}개, 총 ${command.total}원"
                }
                is OrderCommand.Update -> {
                    "주문 수정: 주문 ${command.orderId}, 상품 ${command.items.size}개"
                }
                is OrderCommand.Cancel -> {
                    "주문 취소: 주문 ${command.orderId}, 사유: ${command.reason}"
                }
                is OrderCommand.Ship -> {
                    "배송 시작: 주문 ${command.orderId}, 송장번호 ${command.trackingNumber}"
                }
                is OrderCommand.Complete -> {
                    "주문 완료: 주문 ${command.orderId}"
                }
            }
        }

        val create = OrderCommand.Create(
            1,
            listOf("노트북", "마우스"),
            BigDecimal("1550000")
        )
        assertTrue(executeCommand(create).contains("주문 생성"))

        val update = OrderCommand.Update(100, listOf("노트북", "마우스", "키보드"))
        assertTrue(executeCommand(update).contains("주문 수정"))

        val cancel = OrderCommand.Cancel(100, "재고 부족")
        assertTrue(executeCommand(cancel).contains("재고 부족"))

        val ship = OrderCommand.Ship(100, "1234567890")
        assertTrue(executeCommand(ship).contains("송장번호"))

        val complete = OrderCommand.Complete(100)
        assertTrue(executeCommand(complete).contains("주문 완료"))
    }

    /**
     * 예제 8: 트리 구조 (재귀적 sealed class)
     * JSON과 같은 계층 구조 표현
     */
    sealed class JsonValue {
        data class JsonObject(val values: Map<String, JsonValue>) : JsonValue()
        data class JsonArray(val items: List<JsonValue>) : JsonValue()
        data class JsonString(val value: String) : JsonValue()
        data class JsonNumber(val value: Double) : JsonValue()
        data class JsonBoolean(val value: Boolean) : JsonValue()
        data object JsonNull : JsonValue()
    }

    @Test
    fun `sealed class - 트리 구조 (JSON)`() {
        fun renderJson(value: JsonValue, indent: Int = 0): String {
            val spacing = "  ".repeat(indent)
            return when (value) {
                is JsonValue.JsonObject -> {
                    val entries = value.values.entries.joinToString(",\n") { (key, v) ->
                        "$spacing  \"$key\": ${renderJson(v, indent + 1).trim()}"
                    }
                    "{\n$entries\n$spacing}"
                }
                is JsonValue.JsonArray -> {
                    val items = value.items.joinToString(", ") { renderJson(it, indent).trim() }
                    "[$items]"
                }
                is JsonValue.JsonString -> "\"${value.value}\""
                is JsonValue.JsonNumber -> value.value.toString()
                is JsonValue.JsonBoolean -> value.value.toString()
                is JsonValue.JsonNull -> "null"
            }
        }

        val json = JsonValue.JsonObject(
            mapOf(
                "name" to JsonValue.JsonString("John"),
                "age" to JsonValue.JsonNumber(30.0),
                "active" to JsonValue.JsonBoolean(true),
                "address" to JsonValue.JsonObject(
                    mapOf(
                        "city" to JsonValue.JsonString("Seoul"),
                        "zipcode" to JsonValue.JsonString("12345")
                    )
                ),
                "tags" to JsonValue.JsonArray(
                    listOf(
                        JsonValue.JsonString("developer"),
                        JsonValue.JsonString("kotlin")
                    )
                ),
                "spouse" to JsonValue.JsonNull
            )
        )

        val rendered = renderJson(json)
        assertTrue(rendered.contains("John"))
        assertTrue(rendered.contains("Seoul"))
        assertTrue(rendered.contains("developer"))
        assertTrue(rendered.contains("null"))
    }

    /**
     * 예제 9: 페이지네이션 상태
     * 페이징 처리의 다양한 상태 표현
     */
    sealed class PaginationState<out T> {
        data object Initial : PaginationState<Nothing>()

        data class Loading(
            val isFirstPage: Boolean
        ) : PaginationState<Nothing>()

        data class Success<T>(
            val items: List<T>,
            val currentPage: Int,
            val totalPages: Int,
            val hasMore: Boolean
        ) : PaginationState<T>()

        data class Error(
            val message: String,
            val canRetry: Boolean
        ) : PaginationState<Nothing>()

        data class LoadingMore<T>(
            val currentItems: List<T>
        ) : PaginationState<T>()
    }

    @Test
    fun `sealed class - 페이지네이션 상태`() {
        fun renderPagination(state: PaginationState<String>): String {
            return when (state) {
                is PaginationState.Initial -> {
                    "페이지 초기 상태"
                }
                is PaginationState.Loading -> {
                    if (state.isFirstPage) {
                        "첫 페이지 로딩 중..."
                    } else {
                        "페이지 로딩 중..."
                    }
                }
                is PaginationState.Success -> {
                    "페이지 ${state.currentPage}/${state.totalPages}, " +
                    "아이템 ${state.items.size}개, " +
                    "더보기: ${if (state.hasMore) "가능" else "불가능"}"
                }
                is PaginationState.Error -> {
                    "오류: ${state.message}, 재시도 ${if (state.canRetry) "가능" else "불가능"}"
                }
                is PaginationState.LoadingMore -> {
                    "추가 로딩 중 (현재 ${state.currentItems.size}개)"
                }
            }
        }

        val initial = PaginationState.Initial
        assertEquals("페이지 초기 상태", renderPagination(initial))

        val loading = PaginationState.Loading(isFirstPage = true)
        assertEquals("첫 페이지 로딩 중...", renderPagination(loading))

        val success = PaginationState.Success(
            items = listOf("A", "B", "C"),
            currentPage = 1,
            totalPages = 5,
            hasMore = true
        )
        assertTrue(renderPagination(success).contains("페이지 1/5"))

        val error = PaginationState.Error("Network error", canRetry = true)
        assertTrue(renderPagination(error).contains("재시도 가능"))

        val loadingMore = PaginationState.LoadingMore(listOf("A", "B", "C"))
        assertTrue(renderPagination(loadingMore).contains("현재 3개"))
    }

    /**
     * 예제 10: Either 타입
     * 함수형 프로그래밍의 Either 모나드 구현
     */
    sealed class Either<out L, out R> {
        data class Left<L>(val value: L) : Either<L, Nothing>()
        data class Right<R>(val value: R) : Either<Nothing, R>()

        fun <T> fold(
            onLeft: (@UnsafeVariance L) -> T,
            onRight: (@UnsafeVariance R) -> T
        ): T = when (this) {
            is Left -> onLeft(value)
            is Right -> onRight(value)
        }

        fun <T> map(transform: (@UnsafeVariance R) -> T): Either<L, T> = when (this) {
            is Left -> this
            is Right -> Right(transform(value))
        }

        fun <T> flatMap(transform: (R) -> Either<@UnsafeVariance L, T>): Either<L, T> = when (this) {
            is Left -> this
            is Right -> transform(value)
        }

        fun isLeft(): Boolean = this is Left
        fun isRight(): Boolean = this is Right

        fun getOrNull(): R? = when (this) {
            is Left -> null
            is Right -> value
        }
    }

    @Test
    fun `sealed class - Either 타입`() {
        fun divide(a: Int, b: Int): Either<String, Int> {
            return if (b == 0) {
                Either.Left("0으로 나눌 수 없습니다")
            } else {
                Either.Right(a / b)
            }
        }

        // Right 케이스
        val success = divide(10, 2)
        assertTrue(success.isRight())
        assertEquals(5, success.getOrNull())

        val result = success.fold(
            onLeft = { "Error: $it" },
            onRight = { "Result: $it" }
        )
        assertEquals("Result: 5", result)

        // Left 케이스
        val error = divide(10, 0)
        assertTrue(error.isLeft())
        assertNull(error.getOrNull())

        val errorResult = error.fold(
            onLeft = { "Error: $it" },
            onRight = { "Result: $it" }
        )
        assertEquals("Error: 0으로 나눌 수 없습니다", errorResult)

        // map 테스트
        val mapped = divide(10, 2).map { it * 2 }
        assertEquals(10, mapped.getOrNull())

        // flatMap 테스트
        val chained = divide(10, 2)
            .flatMap { result -> divide(result, 5) }
        assertEquals(1, chained.getOrNull())

        val chainedError = divide(10, 2)
            .flatMap { result -> divide(result, 0) }
        assertTrue(chainedError.isLeft())
    }

    /**
     * 예제 11: 권한/인증 상태
     * 사용자의 인증 및 권한 상태를 표현
     */
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

    enum class MfaMethod {
        SMS, EMAIL, TOTP
    }

    @Test
    fun `sealed class - 권한 인증 상태`() {
        fun checkAccess(state: AuthState, requiredRole: String): Boolean {
            return when (state) {
                is AuthState.Unauthenticated -> false
                is AuthState.Authenticated -> requiredRole in state.roles
                is AuthState.SessionExpired -> false
                is AuthState.RequiresMfa -> false
            }
        }

        val unauthenticated = AuthState.Unauthenticated
        assertFalse(checkAccess(unauthenticated, "ADMIN"))

        val authenticated = AuthState.Authenticated(
            1,
            "john",
            "token123",
            setOf("USER", "ADMIN")
        )
        assertTrue(checkAccess(authenticated, "ADMIN"))
        assertTrue(checkAccess(authenticated, "USER"))
        assertFalse(checkAccess(authenticated, "SUPER_ADMIN"))

        val expired = AuthState.SessionExpired(1)
        assertFalse(checkAccess(expired, "USER"))

        val requiresMfa = AuthState.RequiresMfa(1, MfaMethod.TOTP)
        assertFalse(checkAccess(requiresMfa, "USER"))
    }
}
