package com.example.kotlin.scope_test

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

/**
 * run - 객체 컨텍스트에서 코드 블록을 실행하고 결과를 반환하는 함수
 *
 * 언제 사용하는가?
 * 1. 객체 초기화와 동시에 결과값을 계산할 때 (apply + let의 조합)
 * 2. nullable 객체의 여러 메서드를 호출하고 결과를 받을 때 (?.run)
 * 3. 지역 변수를 묶어서 스코프를 제한하면서 결과를 계산할 때
 * 4. 특정 컨텍스트에서 여러 연산을 수행하고 최종 결과만 필요할 때
 *
 * 특징:
 * - 컨텍스트 객체: this (생략 가능)
 * - 반환값: 람다의 마지막 표현식 결과
 * - 주 용도: 객체 초기화 + 결과 계산, nullable 안전 호출
 * - apply + let을 합친 형태
 */
class RunTest {

    // 실무 예제용 도메인 클래스들
    data class User(
        var id: Long = 0,
        var username: String = "",
        var email: String = "",
        var age: Int = 0,
        var membershipLevel: String = "BASIC"
    )

    data class ShoppingCart(
        val userId: Long,
        val items: MutableList<CartItem> = mutableListOf()
    )

    data class CartItem(
        val productId: Long,
        val productName: String,
        val price: BigDecimal,
        val quantity: Int
    )

    data class Invoice(
        val invoiceNumber: String,
        val items: List<InvoiceItem>,
        val subtotal: BigDecimal,
        val tax: BigDecimal,
        val total: BigDecimal,
        val issueDate: LocalDateTime
    )

    data class InvoiceItem(
        val description: String,
        val quantity: Int,
        val unitPrice: BigDecimal,
        val amount: BigDecimal
    )

    data class Employee(
        val id: Long,
        val name: String,
        val department: String,
        val salary: BigDecimal,
        val joinDate: LocalDate
    )

    /**
     * 예제 1: 기본 사용 - 객체 초기화 후 결과 계산
     */
    @Test
    fun `run - 기본 객체 초기화 및 결과 계산`() {
        // run을 사용하지 않는 경우
        val user1 = User()
        user1.id = 1
        user1.username = "john"
        user1.age = 25
        val isAdult1 = user1.age >= 19

        // run을 사용하는 경우 - 초기화와 계산을 한번에
        val isAdult2 = User().run {
            id = 1
            username = "john"
            age = 25
            age >= 19  // 결과 반환
        }

        assertEquals(isAdult1, isAdult2)
        assertTrue(isAdult2)
    }

    /**
     * 예제 2: nullable 객체와 함께 사용
     * ?. 연산자와 run을 조합해서 안전하게 처리
     */
    @Test
    fun `run - nullable 객체 안전 처리`() {
        val nullableUser: User? = User().apply {
            id = 1
            username = "active_user"
            email = "user@example.com"
            age = 30
        }

        // run으로 nullable 객체의 여러 속성을 사용해서 결과 계산
        val userInfo = nullableUser?.run {
            // this는 non-null User
            """
                User Information:
                - ID: $id
                - Username: $username
                - Email: $email
                - Age: $age
                - Status: ${if (age >= 19) "Adult" else "Minor"}
            """.trimIndent()
        } ?: "No user information available"

        assertTrue(userInfo.contains("active_user"))

        // null인 경우
        val nullUser: User? = null
        val nullInfo = nullUser?.run {
            "ID: $id, Username: $username"
        } ?: "No user information available"

        assertEquals("No user information available", nullInfo)
    }

    /**
     * 예제 3: 복잡한 계산 로직
     * 여러 단계의 계산을 run 내부에서 수행
     */
    @Test
    fun `run - 복잡한 계산 수행`() {
        val cart = ShoppingCart(
            userId = 100,
            items = mutableListOf(
                CartItem(1, "노트북", BigDecimal("1500000"), 1),
                CartItem(2, "마우스", BigDecimal("50000"), 2),
                CartItem(3, "키보드", BigDecimal("120000"), 1)
            )
        )

        // run으로 복잡한 장바구니 계산 수행
        val orderSummary = cart.run {
            // 소계 계산
            val subtotal = items.sumOf { it.price * it.quantity.toBigDecimal() }

            // 할인 계산 (100만원 이상 10% 할인)
            val discount = if (subtotal >= BigDecimal("1000000")) {
                subtotal * BigDecimal("0.1")
            } else {
                BigDecimal.ZERO
            }

            // 배송비 계산 (50만원 이상 무료)
            val shipping = if (subtotal >= BigDecimal("500000")) {
                BigDecimal.ZERO
            } else {
                BigDecimal("3000")
            }

            // 세금 계산 (10%)
            val tax = (subtotal - discount) * BigDecimal("0.1")

            // 최종 금액
            val total = subtotal - discount + shipping + tax

            mapOf(
                "userId" to userId,
                "itemCount" to items.size,
                "subtotal" to subtotal,
                "discount" to discount,
                "shipping" to shipping,
                "tax" to tax,
                "total" to total
            )
        }

        assertEquals(100L, orderSummary["userId"])
        assertEquals(3, orderSummary["itemCount"])
        assertTrue((orderSummary["subtotal"] as BigDecimal) > BigDecimal.ZERO)
        assertTrue((orderSummary["discount"] as BigDecimal) > BigDecimal.ZERO)
        assertEquals(BigDecimal.ZERO, orderSummary["shipping"])
    }

    /**
     * 예제 4: 인보이스 생성
     * 복잡한 문서 생성 로직
     */
    @Test
    fun `run - 인보이스 생성`() {
        data class Order(
            val id: Long,
            val items: List<CartItem>
        )

        val order = Order(
            id = 12345,
            items = listOf(
                CartItem(1, "상품A", BigDecimal("100000"), 2),
                CartItem(2, "상품B", BigDecimal("50000"), 3),
                CartItem(3, "상품C", BigDecimal("75000"), 1)
            )
        )

        // run으로 주문에서 인보이스 생성
        val invoice = order.run {
            // 인보이스 아이템 생성
            val invoiceItems = items.map { item ->
                InvoiceItem(
                    description = item.productName,
                    quantity = item.quantity,
                    unitPrice = item.price,
                    amount = item.price * item.quantity.toBigDecimal()
                )
            }

            // 금액 계산
            val subtotal = invoiceItems.sumOf { it.amount }
            val taxRate = BigDecimal("0.1")
            val tax = subtotal * taxRate
            val total = subtotal + tax

            Invoice(
                invoiceNumber = "INV-$id",
                items = invoiceItems,
                subtotal = subtotal,
                tax = tax,
                total = total,
                issueDate = LocalDateTime.now()
            )
        }

        assertEquals("INV-12345", invoice.invoiceNumber)
        assertEquals(3, invoice.items.size)
        assertEquals(BigDecimal("425000"), invoice.subtotal)
        assertEquals(BigDecimal("42500.0"), invoice.tax)
        assertEquals(BigDecimal("467500.0"), invoice.total)
    }

    /**
     * 예제 5: 데이터 검증 및 변환
     */
    @Test
    fun `run - 데이터 검증 및 변환`() {
        data class RegistrationData(
            val username: String,
            val email: String,
            val password: String,
            val age: Int
        )

        data class ValidationResult(
            val isValid: Boolean,
            val errors: List<String>,
            val user: User?
        )

        fun validateAndCreateUser(data: RegistrationData): ValidationResult {
            return data.run {
                val errors = mutableListOf<String>()

                // 검증 로직
                if (username.length < 3) {
                    errors.add("Username must be at least 3 characters")
                }
                if (!email.contains("@")) {
                    errors.add("Invalid email format")
                }
                if (password.length < 8) {
                    errors.add("Password must be at least 8 characters")
                }
                if (age < 19) {
                    errors.add("Must be at least 19 years old")
                }

                // 검증 통과 시 User 생성
                val user = if (errors.isEmpty()) {
                    User().apply {
                        this.username = data.username
                        this.email = data.email
                        this.age = data.age
                    }
                } else {
                    null
                }

                ValidationResult(
                    isValid = errors.isEmpty(),
                    errors = errors,
                    user = user
                )
            }
        }

        // 유효한 데이터
        val validData = RegistrationData("johndoe", "john@example.com", "password123", 25)
        val validResult = validateAndCreateUser(validData)

        assertTrue(validResult.isValid)
        assertEquals(0, validResult.errors.size)
        assertNotNull(validResult.user)
        assertEquals("johndoe", validResult.user?.username)

        // 유효하지 않은 데이터
        val invalidData = RegistrationData("ab", "invalid-email", "short", 17)
        val invalidResult = validateAndCreateUser(invalidData)

        assertFalse(invalidResult.isValid)
        assertEquals(4, invalidResult.errors.size)
        assertNull(invalidResult.user)
    }

    /**
     * 예제 6: 스코프 제한을 통한 복잡한 계산
     */
    @Test
    fun `run - 스코프 제한 복잡한 계산`() {
        // 여러 변수를 사용하는 복잡한 계산을 run으로 스코프 제한
        val statistics = run {
            val numbers = listOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100)

            val sum = numbers.sum()
            val count = numbers.size
            val avg = sum.toDouble() / count
            val max = numbers.maxOrNull() ?: 0
            val min = numbers.minOrNull() ?: 0

            val variance = numbers.map { (it - avg).let { diff -> diff * diff } }.average()
            val stdDev = kotlin.math.sqrt(variance)

            val median = numbers.sorted().let {
                if (it.size % 2 == 0) {
                    (it[it.size / 2 - 1] + it[it.size / 2]) / 2.0
                } else {
                    it[it.size / 2].toDouble()
                }
            }

            mapOf(
                "count" to count,
                "sum" to sum,
                "average" to avg,
                "max" to max,
                "min" to min,
                "median" to median,
                "stdDev" to stdDev
            )
        }

        assertEquals(10, statistics["count"])
        assertEquals(550, statistics["sum"])
        assertEquals(55.0, statistics["average"])
        assertEquals(100, statistics["max"])
        assertEquals(10, statistics["min"])

        // sum, avg 등의 중간 변수는 여기서 접근 불가 (스코프 제한됨)
    }

    /**
     * 예제 7: 조건부 객체 생성
     */
    @Test
    fun `run - 조건부 객체 생성 및 초기화`() {
        fun createUserByType(type: String, name: String): User {
            return User().run {
                this.username = name

                when (type) {
                    "PREMIUM" -> {
                        membershipLevel = "PREMIUM"
                        // 프리미엄 사용자는 자동으로 높은 ID 할당
                        id = 1000 + System.currentTimeMillis() % 1000
                    }
                    "VIP" -> {
                        membershipLevel = "VIP"
                        id = 5000 + System.currentTimeMillis() % 1000
                    }
                    else -> {
                        membershipLevel = "BASIC"
                        id = System.currentTimeMillis() % 1000
                    }
                }

                // 이메일은 멤버십 레벨에 따라 다른 도메인 사용
                email = when (membershipLevel) {
                    "VIP" -> "$username@vip.example.com"
                    "PREMIUM" -> "$username@premium.example.com"
                    else -> "$username@example.com"
                }

                this  // User 객체 반환
            }
        }

        val basicUser = createUserByType("BASIC", "basic_user")
        assertEquals("BASIC", basicUser.membershipLevel)
        assertEquals("basic_user@example.com", basicUser.email)

        val premiumUser = createUserByType("PREMIUM", "premium_user")
        assertEquals("PREMIUM", premiumUser.membershipLevel)
        assertEquals("premium_user@premium.example.com", premiumUser.email)
        assertTrue(premiumUser.id >= 1000)

        val vipUser = createUserByType("VIP", "vip_user")
        assertEquals("VIP", vipUser.membershipLevel)
        assertEquals("vip_user@vip.example.com", vipUser.email)
        assertTrue(vipUser.id >= 5000)
    }

    /**
     * 예제 8: 데이터베이스 트랜잭션 시뮬레이션
     */
    @Test
    fun `run - 트랜잭션 처리 시뮬레이션`() {
        data class Account(
            val id: Long,
            var balance: BigDecimal
        )

        data class TransactionResult(
            val success: Boolean,
            val message: String,
            val fromAccountBalance: BigDecimal,
            val toAccountBalance: BigDecimal
        )

        fun transfer(
            from: Account,
            to: Account,
            amount: BigDecimal
        ): TransactionResult {
            return run {
                // 트랜잭션 시작
                val originalFromBalance = from.balance
                val originalToBalance = to.balance

                try {
                    // 검증
                    if (amount <= BigDecimal.ZERO) {
                        return@run TransactionResult(
                            success = false,
                            message = "Transfer amount must be positive",
                            fromAccountBalance = from.balance,
                            toAccountBalance = to.balance
                        )
                    }

                    if (from.balance < amount) {
                        return@run TransactionResult(
                            success = false,
                            message = "Insufficient balance",
                            fromAccountBalance = from.balance,
                            toAccountBalance = to.balance
                        )
                    }

                    // 송금 실행
                    from.balance -= amount
                    to.balance += amount

                    TransactionResult(
                        success = true,
                        message = "Transfer successful",
                        fromAccountBalance = from.balance,
                        toAccountBalance = to.balance
                    )
                } catch (e: Exception) {
                    // 롤백
                    from.balance = originalFromBalance
                    to.balance = originalToBalance

                    TransactionResult(
                        success = false,
                        message = "Transfer failed: ${e.message}",
                        fromAccountBalance = from.balance,
                        toAccountBalance = to.balance
                    )
                }
            }
        }

        val account1 = Account(1, BigDecimal("100000"))
        val account2 = Account(2, BigDecimal("50000"))

        // 성공 케이스
        val result1 = transfer(account1, account2, BigDecimal("30000"))
        assertTrue(result1.success)
        assertEquals(BigDecimal("70000"), account1.balance)
        assertEquals(BigDecimal("80000"), account2.balance)

        // 실패 케이스 - 잔액 부족
        val result2 = transfer(account1, account2, BigDecimal("100000"))
        assertFalse(result2.success)
        assertTrue(result2.message.contains("Insufficient"))
        assertEquals(BigDecimal("70000"), account1.balance)  // 변경 없음
    }

    /**
     * 예제 9: 리포트 생성
     */
    @Test
    fun `run - 복잡한 리포트 생성`() {
        val employees = listOf(
            Employee(1, "김철수", "개발팀", BigDecimal("7000000"), LocalDate.of(2020, 1, 15)),
            Employee(2, "이영희", "개발팀", BigDecimal("5000000"), LocalDate.of(2021, 6, 1)),
            Employee(3, "박민수", "디자인팀", BigDecimal("6000000"), LocalDate.of(2019, 3, 20)),
            Employee(4, "정지원", "디자인팀", BigDecimal("5500000"), LocalDate.of(2020, 9, 10)),
            Employee(5, "최수진", "기획팀", BigDecimal("6500000"), LocalDate.of(2018, 11, 5))
        )

        val report = employees.run {
            val totalEmployees = size
            val totalSalary = sumOf { it.salary }
            val avgSalary = totalSalary.divide(
                size.toBigDecimal(),
                2,
                RoundingMode.HALF_UP
            )

            // 부서별 통계
            val byDepartment = groupBy { it.department }
                .mapValues { (_, emps) ->
                    mapOf(
                        "count" to emps.size,
                        "totalSalary" to emps.sumOf { it.salary },
                        "avgSalary" to emps.sumOf { it.salary }
                            .divide(emps.size.toBigDecimal(), 2, RoundingMode.HALF_UP)
                    )
                }

            // 근속년수 통계
            val avgYearsOfService = map { emp ->
                Period.between(emp.joinDate, LocalDate.now()).years
            }.average()

            // 가장 오래된 직원
            val seniorEmployee = minByOrNull { it.joinDate }

            // 최고/최저 급여
            val maxSalary = maxOf { it.salary }
            val minSalary = minOf { it.salary }

            """
                ====================================
                직원 통계 리포트
                ====================================

                [전체 통계]
                총 직원 수: ${totalEmployees}명
                총 급여: ${totalSalary}원
                평균 급여: ${avgSalary}원
                최고 급여: ${maxSalary}원
                최저 급여: ${minSalary}원
                평균 근속년수: ${"%.1f".format(avgYearsOfService)}년

                [부서별 통계]
                ${byDepartment.entries.joinToString("\n") { (dept, stats) ->
                    "- $dept: ${stats["count"]}명, 평균급여 ${stats["avgSalary"]}원"
                }}

                [최고 근속자]
                이름: ${seniorEmployee?.name}
                부서: ${seniorEmployee?.department}
                입사일: ${seniorEmployee?.joinDate}
                근속기간: ${seniorEmployee?.let { Period.between(it.joinDate, LocalDate.now()).years }}년

                ====================================
            """.trimIndent()
        }

        assertTrue(report.contains("총 직원 수: 5명"))
        assertTrue(report.contains("개발팀"))
        assertTrue(report.contains("디자인팀"))
        assertTrue(report.contains("기획팀"))
        println(report)
    }

    /**
     * 예제 10: 실무 시나리오 - API 응답 처리 및 변환
     */
    @Test
    fun `run - 실무 시나리오 API 응답 처리`() {
        data class ApiResponse(
            val statusCode: Int,
            val headers: Map<String, String>,
            val body: String?,
            val timestamp: LocalDateTime
        )

        data class ParsedResponse<T>(
            val success: Boolean,
            val data: T?,
            val error: String?,
            val metadata: Map<String, Any>
        )

        fun parseUserResponse(response: ApiResponse): ParsedResponse<User> {
            return response.run {
                // 상태 코드 확인
                if (statusCode !in 200..299) {
                    return@run ParsedResponse(
                        success = false,
                        data = null,
                        error = "HTTP Error: $statusCode",
                        metadata = mapOf(
                            "statusCode" to statusCode,
                            "timestamp" to timestamp
                        )
                    )
                }

                // 바디 파싱
                val user = body?.run {
                    // 실제로는 JSON 파싱, 여기서는 간단히 시뮬레이션
                    if (isEmpty()) return@run null

                    val parts = split("|")
                    if (parts.size < 3) return@run null

                    User().apply {
                        id = parts[0].toLongOrNull() ?: 0
                        username = parts[1]
                        email = parts[2]
                    }
                }

                if (user == null) {
                    return@run ParsedResponse(
                        success = false,
                        data = null,
                        error = "Failed to parse response body",
                        metadata = mapOf(
                            "statusCode" to statusCode,
                            "bodyLength" to (body?.length ?: 0)
                        )
                    )
                }

                // 성공
                ParsedResponse(
                    success = true,
                    data = user,
                    error = null,
                    metadata = mapOf(
                        "statusCode" to statusCode,
                        "contentType" to (headers["Content-Type"] ?: "unknown"),
                        "timestamp" to timestamp,
                        "processingTime" to Period.between(
                            timestamp.toLocalDate(),
                            LocalDateTime.now().toLocalDate()
                        ).days
                    )
                )
            }
        }

        // 성공 케이스
        val successResponse = ApiResponse(
            statusCode = 200,
            headers = mapOf("Content-Type" to "application/json"),
            body = "123|johndoe|john@example.com",
            timestamp = LocalDateTime.now()
        )

        val parsed = parseUserResponse(successResponse)
        assertTrue(parsed.success)
        assertNotNull(parsed.data)
        assertEquals("johndoe", parsed.data?.username)

        // 실패 케이스 - HTTP 에러
        val errorResponse = ApiResponse(
            statusCode = 404,
            headers = emptyMap(),
            body = null,
            timestamp = LocalDateTime.now()
        )

        val errorParsed = parseUserResponse(errorResponse)
        assertFalse(errorParsed.success)
        assertNull(errorParsed.data)
        assertTrue(errorParsed.error?.contains("404") ?: false)

        // 실패 케이스 - 파싱 에러
        val parseErrorResponse = ApiResponse(
            statusCode = 200,
            headers = emptyMap(),
            body = "invalid-data",
            timestamp = LocalDateTime.now()
        )

        val parseErrorParsed = parseUserResponse(parseErrorResponse)
        assertFalse(parseErrorParsed.success)
        assertTrue(parseErrorParsed.error?.contains("parse") ?: false)
    }

    /**
     * 예제 11: 성능 최적화 - 지연 계산
     */
    @Test
    fun `run - 조건부 지연 계산`() {
        data class ExpensiveResult(
            val value: String,
            val computationTime: Long
        )

        fun expensiveComputation(input: String): ExpensiveResult {
            val startTime = System.currentTimeMillis()
            Thread.sleep(50)  // 무거운 계산 시뮬레이션
            val endTime = System.currentTimeMillis()

            return ExpensiveResult(
                value = input.uppercase().reversed(),
                computationTime = endTime - startTime
            )
        }

        fun processData(data: String?, needsProcessing: Boolean): String {
            return data?.run {
                if (!needsProcessing) {
                    // run 블록 내부지만 조건에 맞지 않으면 조기 반환
                    return@run this
                }

                // 조건에 맞을 때만 무거운 계산 수행
                val result = expensiveComputation(this)
                "Processed: ${result.value} (took ${result.computationTime}ms)"
            } ?: "No data"
        }

        // 처리 불필요 - 빠름
        val startTime1 = System.currentTimeMillis()
        val result1 = processData("hello", needsProcessing = false)
        val time1 = System.currentTimeMillis() - startTime1

        assertEquals("hello", result1)
        assertTrue(time1 < 10)  // 거의 즉시

        // 처리 필요 - 느림
        val startTime2 = System.currentTimeMillis()
        val result2 = processData("hello", needsProcessing = true)
        val time2 = System.currentTimeMillis() - startTime2

        assertTrue(result2.contains("OLLEH"))
        assertTrue(time2 >= 50)  // 최소 50ms
    }

    /**
     * 예제 12: 체이닝과 함께 사용
     */
    @Test
    fun `run - 복잡한 체이닝`() {
        data class Product(
            val id: Long,
            val name: String,
            val price: BigDecimal,
            val category: String
        )

        val products = listOf(
            Product(1, "노트북", BigDecimal("1500000"), "전자기기"),
            Product(2, "마우스", BigDecimal("30000"), "전자기기"),
            Product(3, "책상", BigDecimal("200000"), "가구"),
            Product(4, "의자", BigDecimal("150000"), "가구"),
            Product(5, "키보드", BigDecimal("80000"), "전자기기")
        )

        val summary = products
            .groupBy { it.category }
            .run {
                // 카테고리별로 그룹화된 맵을 처리
                mapValues { (category, items) ->
                    items.run {
                        // 각 카테고리의 상품 리스트를 처리
                        val totalPrice = sumOf { it.price }
                        val avgPrice = totalPrice.divide(
                            size.toBigDecimal(),
                            2,
                            RoundingMode.HALF_UP
                        )
                        val productNames = map { it.name }

                        mapOf(
                            "category" to category,
                            "count" to size,
                            "totalPrice" to totalPrice,
                            "avgPrice" to avgPrice,
                            "products" to productNames
                        )
                    }
                }
            }
            .run {
                // 최종 요약 생성
                val totalCategories = size
                val totalProducts = values.sumOf { it["count"] as Int }
                val grandTotal = values.sumOf { it["totalPrice"] as BigDecimal }

                """
                    카테고리 수: $totalCategories
                    총 상품 수: $totalProducts
                    총 가격: ${grandTotal}원

                    ${entries.joinToString("\n\n") { (cat, info) ->
                        """
                        [${info["category"]}]
                        - 상품 수: ${info["count"]}개
                        - 평균 가격: ${info["avgPrice"]}원
                        - 상품 목록: ${(info["products"] as List<*>).joinToString(", ")}
                        """.trimIndent()
                    }}
                """.trimIndent()
            }

        assertTrue(summary.contains("카테고리 수: 2"))
        assertTrue(summary.contains("총 상품 수: 5"))
        assertTrue(summary.contains("전자기기"))
        assertTrue(summary.contains("가구"))
        println(summary)
    }
}
