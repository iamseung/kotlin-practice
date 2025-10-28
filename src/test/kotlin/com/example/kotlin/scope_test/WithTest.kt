package com.example.kotlin.scope_test

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

/**
 * with - 객체의 컨텍스트 내에서 여러 작업을 수행하고 결과를 반환하는 함수
 *
 * 언제 사용하는가?
 * 1. 특정 객체의 여러 함수나 속성을 연속으로 호출할 때
 * 2. 객체의 데이터를 사용해서 계산이나 변환을 수행할 때
 * 3. "이 객체로(with) 다음 작업을 수행하라"는 의미가 명확할 때
 *
 * 특징:
 * - 컨텍스트 객체: this (생략 가능)
 * - 반환값: 람다의 마지막 표현식 결과
 * - 주 용도: 객체의 함수 호출, 데이터 접근 및 변환
 * - 주의: non-null 객체에만 사용 (nullable이면 ?.run 사용)
 */
class WithTest {

    // 실무 예제용 도메인 클래스들
    data class Product(
        val id: Long,
        val name: String,
        val price: BigDecimal,
        val quantity: Int,
        val category: String,
        val discount: BigDecimal = BigDecimal.ZERO
    )

    data class Order(
        val id: Long,
        val customerId: Long,
        val items: List<OrderItem>,
        val orderDate: LocalDateTime,
        val shippingAddress: Address,
        val status: String
    )

    data class OrderItem(
        val productId: Long,
        val productName: String,
        val price: BigDecimal,
        val quantity: Int,
        val discount: BigDecimal = BigDecimal.ZERO
    )

    data class Address(
        val street: String,
        val city: String,
        val state: String,
        val zipCode: String,
        val country: String
    )

    data class Employee(
        val id: Long,
        val name: String,
        val department: String,
        val salary: BigDecimal,
        val hireDate: LocalDate,
        val position: String
    )

    data class Point(val x: Double, val y: Double)
    data class Rectangle(val topLeft: Point, val bottomRight: Point)

    /**
     * 예제 1: 기본 사용 - 객체의 속성 접근
     */
    @Test
    fun `with - 기본 객체 속성 접근`() {
        val product = Product(
            id = 1,
            name = "노트북",
            price = BigDecimal("1500000"),
            quantity = 10,
            category = "전자기기"
        )

        // with 없이 사용
        val descriptionWithout = "${product.name}은(는) ${product.category} 카테고리의 상품입니다. " +
                "가격: ${product.price}원, 재고: ${product.quantity}개"

        // with 사용 - this로 객체에 접근 (this는 생략 가능)
        val descriptionWith = with(product) {
            "${name}은(는) $category 카테고리의 상품입니다. " +
                    "가격: ${price}원, 재고: ${quantity}개"
        }

        assertEquals(descriptionWithout, descriptionWith)
    }

    /**
     * 예제 2: 복잡한 계산 수행
     * 주문 객체에서 총액, 할인액, 최종 금액 계산
     */
    @Test
    fun `with - 주문 금액 계산`() {
        val order = Order(
            id = 1001,
            customerId = 500,
            items = listOf(
                OrderItem(1, "노트북", BigDecimal("1500000"), 2, BigDecimal("0.1")),
                OrderItem(2, "마우스", BigDecimal("50000"), 3, BigDecimal("0.05")),
                OrderItem(3, "키보드", BigDecimal("120000"), 1, BigDecimal("0.15"))
            ),
            orderDate = LocalDateTime.now(),
            shippingAddress = Address("강남대로 123", "서울", "강남구", "06000", "대한민국"),
            status = "PENDING"
        )

        // with를 사용해서 주문의 여러 계산을 수행
        val orderSummary = with(order) {
            // 각 아이템의 소계 계산 (할인 적용 전)
            val subtotal = items.sumOf { it.price * it.quantity.toBigDecimal() }

            // 총 할인액 계산
            val totalDiscount = items.sumOf { item ->
                val itemTotal = item.price * item.quantity.toBigDecimal()
                itemTotal * item.discount
            }

            // 최종 금액
            val finalAmount = subtotal - totalDiscount

            // 배송비 (100만원 이상 무료)
            val shippingFee = if (finalAmount >= BigDecimal("1000000")) {
                BigDecimal.ZERO
            } else {
                BigDecimal("3000")
            }

            val grandTotal = finalAmount + shippingFee

            // 결과 맵 반환
            mapOf(
                "orderId" to id,
                "itemCount" to items.size,
                "subtotal" to subtotal,
                "discount" to totalDiscount,
                "shippingFee" to shippingFee,
                "total" to grandTotal,
                "destination" to "${shippingAddress.city}, ${shippingAddress.state}"
            )
        }

        assertEquals(1001L, orderSummary["orderId"])
        assertEquals(3, orderSummary["itemCount"])
        assertTrue((orderSummary["subtotal"] as BigDecimal) > BigDecimal.ZERO)
        println("주문 요약: $orderSummary")
    }

    /**
     * 예제 3: 문자열 빌더를 사용한 리포트 생성
     * StringBuilder의 여러 메서드를 연속으로 호출
     */
    @Test
    fun `with - StringBuilder로 상세 리포트 생성`() {
        val employee = Employee(
            id = 1,
            name = "김철수",
            department = "개발팀",
            salary = BigDecimal("7000000"),
            hireDate = LocalDate.of(2020, 3, 15),
            position = "시니어 개발자"
        )

        val report = with(StringBuilder()) {
            appendLine("=".repeat(50))
            appendLine("직원 정보 상세 리포트")
            appendLine("=".repeat(50))
            appendLine()

            appendLine("[기본 정보]")
            appendLine("사번: ${employee.id}")
            appendLine("이름: ${employee.name}")
            appendLine("직급: ${employee.position}")
            appendLine("부서: ${employee.department}")
            appendLine()

            appendLine("[급여 정보]")
            val yearlySalary = employee.salary * BigDecimal("12")
            appendLine("월급: ${employee.salary}원")
            appendLine("연봉: ${yearlySalary}원")
            appendLine()

            appendLine("[근속 정보]")
            val workPeriod = Period.between(employee.hireDate, LocalDate.now())
            appendLine("입사일: ${employee.hireDate}")
            appendLine("근속기간: ${workPeriod.years}년 ${workPeriod.months}개월")
            appendLine()

            appendLine("=".repeat(50))

            toString()  // 마지막 표현식이 반환됨
        }

        assertTrue(report.contains("김철수"))
        assertTrue(report.contains("시니어 개발자"))
        assertTrue(report.contains("근속기간"))
        println(report)
    }

    /**
     * 예제 4: 기하학 계산
     * 도형 객체의 여러 속성을 계산
     */
    @Test
    fun `with - 사각형의 여러 속성 계산`() {
        val rectangle = Rectangle(
            topLeft = Point(0.0, 10.0),
            bottomRight = Point(10.0, 0.0)
        )

        val properties = with(rectangle) {
            val width = bottomRight.x - topLeft.x
            val height = topLeft.y - bottomRight.y
            val area = width * height
            val perimeter = 2 * (width + height)
            val diagonal = kotlin.math.sqrt(width * width + height * height)
            val center = Point(
                (topLeft.x + bottomRight.x) / 2,
                (topLeft.y + bottomRight.y) / 2
            )

            mapOf(
                "width" to width,
                "height" to height,
                "area" to area,
                "perimeter" to perimeter,
                "diagonal" to diagonal,
                "center" to center
            )
        }

        assertEquals(10.0, properties["width"])
        assertEquals(10.0, properties["height"])
        assertEquals(100.0, properties["area"])
        assertEquals(40.0, properties["perimeter"])
        assertEquals(Point(5.0, 5.0), properties["center"])
    }

    /**
     * 예제 5: 리스트를 사용한 통계 계산
     */
    @Test
    fun `with - 리스트 통계 계산`() {
        val products = listOf(
            Product(1, "상품A", BigDecimal("10000"), 100, "카테고리1"),
            Product(2, "상품B", BigDecimal("20000"), 50, "카테고리1"),
            Product(3, "상품C", BigDecimal("15000"), 75, "카테고리2"),
            Product(4, "상품D", BigDecimal("30000"), 30, "카테고리2"),
            Product(5, "상품E", BigDecimal("25000"), 40, "카테고리3")
        )

        val statistics = with(products) {
            val totalProducts = size
            val totalValue = sumOf { it.price * it.quantity.toBigDecimal() }
            val avgPrice = sumOf { it.price }.divide(size.toBigDecimal(), 2, RoundingMode.HALF_UP)
            val maxPrice = maxOf { it.price }
            val minPrice = minOf { it.price }
            val totalInventory = sumOf { it.quantity }

            val byCategory = groupBy { it.category }
                .mapValues { (_, items) ->
                    mapOf(
                        "count" to items.size,
                        "totalValue" to items.sumOf { it.price * it.quantity.toBigDecimal() }
                    )
                }

            mapOf(
                "totalProducts" to totalProducts,
                "totalValue" to totalValue,
                "averagePrice" to avgPrice,
                "maxPrice" to maxPrice,
                "minPrice" to minPrice,
                "totalInventory" to totalInventory,
                "byCategory" to byCategory
            )
        }

        assertEquals(5, statistics["totalProducts"])
        assertEquals(295, statistics["totalInventory"])
        assertTrue((statistics["totalValue"] as BigDecimal) > BigDecimal.ZERO)

        @Suppress("UNCHECKED_CAST")
        val byCategory = statistics["byCategory"] as Map<String, Map<String, Any>>
        assertEquals(3, byCategory.size)
        println("통계: $statistics")
    }

    /**
     * 예제 6: Map을 사용한 데이터 변환
     */
    @Test
    fun `with - Map 데이터 처리 및 변환`() {
        val userPreferences = mapOf(
            "theme" to "dark",
            "language" to "ko",
            "fontSize" to 14,
            "notifications" to mapOf(
                "email" to true,
                "push" to false,
                "sms" to true
            ),
            "privacy" to mapOf(
                "showProfile" to true,
                "showEmail" to false
            )
        )

        val summary = with(userPreferences) {
            val theme = get("theme") as? String ?: "light"
            val language = get("language") as? String ?: "en"
            val fontSize = get("fontSize") as? Int ?: 12

            @Suppress("UNCHECKED_CAST")
            val notifications = get("notifications") as? Map<String, Boolean> ?: emptyMap()
            val enabledNotifications = notifications.filter { it.value }.keys

            @Suppress("UNCHECKED_CAST")
            val privacy = get("privacy") as? Map<String, Boolean> ?: emptyMap()
            val publicSettings = privacy.filter { it.value }.keys

            """
                사용자 설정 요약:
                - 테마: $theme
                - 언어: $language
                - 글꼴 크기: ${fontSize}px
                - 활성화된 알림: ${enabledNotifications.joinToString(", ")}
                - 공개 설정: ${publicSettings.joinToString(", ")}
            """.trimIndent()
        }

        assertTrue(summary.contains("dark"))
        assertTrue(summary.contains("ko"))
        assertTrue(summary.contains("email"))
        println(summary)
    }

    /**
     * 예제 7: 중첩된 with 사용
     * 여러 객체를 순차적으로 처리
     */
    @Test
    fun `with - 중첩된 with로 복잡한 처리`() {
        val order = Order(
            id = 2001,
            customerId = 100,
            items = listOf(
                OrderItem(1, "프리미엄 노트북", BigDecimal("2000000"), 1, BigDecimal("0.1")),
                OrderItem(2, "무선 마우스", BigDecimal("80000"), 2, BigDecimal("0.05"))
            ),
            orderDate = LocalDateTime.now(),
            shippingAddress = Address(
                "테헤란로 427",
                "서울",
                "강남구",
                "06158",
                "대한민국"
            ),
            status = "CONFIRMED"
        )

        val invoice = with(order) {
            val customerInfo = "고객 ID: $customerId"

            val itemsDetail = with(items) {
                joinToString("\n") { item ->
                    with(item) {
                        val itemTotal = price * quantity.toBigDecimal()
                        val discountAmount = itemTotal * discount
                        val finalPrice = itemTotal - discountAmount
                        "- $productName x$quantity = ${itemTotal}원 (할인: ${discountAmount}원, 최종: ${finalPrice}원)"
                    }
                }
            }

            val addressInfo = with(shippingAddress) {
                "$country $state $city $street ($zipCode)"
            }

            val total = items.sumOf { item ->
                val itemTotal = item.price * item.quantity.toBigDecimal()
                itemTotal - (itemTotal * item.discount)
            }

            """
                ====== 주문서 ======
                주문 번호: $id
                주문 일시: $orderDate
                상태: $status

                $customerInfo

                [주문 상품]
                $itemsDetail

                [배송지]
                $addressInfo

                [결제 금액]
                총액: ${total}원
                ===================
            """.trimIndent()
        }

        assertTrue(invoice.contains("주문 번호: 2001"))
        assertTrue(invoice.contains("프리미엄 노트북"))
        assertTrue(invoice.contains("테헤란로 427"))
        println(invoice)
    }

    /**
     * 예제 8: with와 다른 scope function 조합
     */
    @Test
    fun `with - 다른 scope function과 조합`() {
        val employees = listOf(
            Employee(1, "김철수", "개발팀", BigDecimal("7000000"), LocalDate.of(2020, 1, 1), "시니어"),
            Employee(2, "이영희", "개발팀", BigDecimal("5000000"), LocalDate.of(2021, 6, 1), "주니어"),
            Employee(3, "박민수", "디자인팀", BigDecimal("6000000"), LocalDate.of(2019, 3, 1), "시니어"),
            Employee(4, "정지원", "기획팀", BigDecimal("5500000"), LocalDate.of(2020, 9, 1), "주니어")
        )

        // with 내에서 let, apply 등과 조합
        val departmentReport = with(employees) {
            groupBy { it.department }
                .mapValues { (department, members) ->
                    with(members) {
                        mapOf(
                            "department" to department,
                            "headCount" to size,
                            "totalSalary" to sumOf { it.salary },
                            "avgSalary" to sumOf { it.salary }.divide(
                                size.toBigDecimal(),
                                2,
                                RoundingMode.HALF_UP
                            ),
                            "members" to map { it.name }
                        )
                    }
                }
        }

        assertEquals(3, departmentReport.size)
        assertTrue(departmentReport.containsKey("개발팀"))

        val devTeam = departmentReport["개발팀"]!!
        assertEquals(2, devTeam["headCount"])

        @Suppress("UNCHECKED_CAST")
        val devMembers = devTeam["members"] as List<String>
        assertTrue(devMembers.contains("김철수"))

        println("부서별 리포트: $departmentReport")
    }

    /**
     * 예제 9: with를 사용한 데이터 검증
     */
    @Test
    fun `with - 복잡한 데이터 검증`() {
        val order = Order(
            id = 3001,
            customerId = 200,
            items = listOf(
                OrderItem(1, "상품1", BigDecimal("50000"), 2),
                OrderItem(2, "상품2", BigDecimal("30000"), 1)
            ),
            orderDate = LocalDateTime.now(),
            shippingAddress = Address("주소1", "서울", "강남구", "12345", "대한민국"),
            status = "PENDING"
        )

        val validationResult = with(order) {
            val errors = mutableListOf<String>()

            // 주문 아이템 검증
            if (items.isEmpty()) {
                errors.add("주문 상품이 없습니다")
            }

            // 가격 검증
            items.forEach { item ->
                if (item.price <= BigDecimal.ZERO) {
                    errors.add("${item.productName}의 가격이 유효하지 않습니다")
                }
                if (item.quantity <= 0) {
                    errors.add("${item.productName}의 수량이 유효하지 않습니다")
                }
            }

            // 주소 검증
            with(shippingAddress) {
                if (street.isBlank()) errors.add("주소가 입력되지 않았습니다")
                if (city.isBlank()) errors.add("도시가 입력되지 않았습니다")
                if (zipCode.isBlank()) errors.add("우편번호가 입력되지 않았습니다")
            }

            // 최소 주문 금액 검증 (10,000원)
            val totalAmount = items.sumOf { it.price * it.quantity.toBigDecimal() }
            if (totalAmount < BigDecimal("10000")) {
                errors.add("최소 주문 금액(10,000원)을 만족하지 않습니다")
            }

            mapOf(
                "isValid" to errors.isEmpty(),
                "errors" to errors,
                "totalAmount" to totalAmount
            )
        }

        assertTrue(validationResult["isValid"] as Boolean)

        @Suppress("UNCHECKED_CAST")
        val errors = validationResult["errors"] as List<String>
        assertEquals(0, errors.size)
    }

    /**
     * 예제 10: 실무 시나리오 - 대시보드 데이터 집계
     */
    @Test
    fun `with - 대시보드 데이터 집계`() {
        // 실제 서비스의 여러 메트릭 데이터
        val metricsData = mapOf(
            "users" to mapOf(
                "total" to 150000,
                "active" to 45000,
                "new_today" to 1200,
                "premium" to 5000
            ),
            "orders" to mapOf(
                "total" to 250000,
                "today" to 3500,
                "pending" to 450,
                "completed" to 248000
            ),
            "revenue" to mapOf(
                "total" to BigDecimal("50000000000"),
                "today" to BigDecimal("150000000"),
                "thisMonth" to BigDecimal("4500000000")
            ),
            "products" to mapOf(
                "total" to 5000,
                "inStock" to 4200,
                "lowStock" to 300,
                "outOfStock" to 500
            )
        )

        // with를 사용해서 대시보드용 요약 데이터 생성
        val dashboardSummary = with(metricsData) {
            @Suppress("UNCHECKED_CAST")
            val users = get("users") as Map<String, Int>
            @Suppress("UNCHECKED_CAST")
            val orders = get("orders") as Map<String, Int>
            @Suppress("UNCHECKED_CAST")
            val revenue = get("revenue") as Map<String, BigDecimal>
            @Suppress("UNCHECKED_CAST")
            val products = get("products") as Map<String, Int>

            // 각 섹션별 요약 정보 생성
            val usersSummary = with(users) {
                mapOf(
                    "activeRate" to String.format(
                        "%.1f%%",
                        (get("active")!! * 100.0 / get("total")!!)
                    ),
                    "premiumRate" to String.format(
                        "%.1f%%",
                        (get("premium")!! * 100.0 / get("total")!!)
                    ),
                    "todayGrowth" to get("new_today")
                )
            }

            val ordersSummary = with(orders) {
                mapOf(
                    "completionRate" to String.format(
                        "%.1f%%",
                        (get("completed")!! * 100.0 / get("total")!!)
                    ),
                    "pendingCount" to get("pending"),
                    "todayOrders" to get("today")
                )
            }

            val revenueSummary = with(revenue) {
                val todayRevenue = get("today")!!
                val thisMonthRevenue = get("thisMonth")!!
                val avgDailyRevenue = thisMonthRevenue.divide(
                    BigDecimal("30"),
                    2,
                    RoundingMode.HALF_UP
                )

                mapOf(
                    "todayRevenue" to todayRevenue,
                    "monthlyRevenue" to thisMonthRevenue,
                    "avgDailyRevenue" to avgDailyRevenue,
                    "todayVsAvg" to String.format(
                        "%.1f%%",
                        ((todayRevenue - avgDailyRevenue) * BigDecimal("100") / avgDailyRevenue).toDouble()
                    )
                )
            }

            val productsSummary = with(products) {
                mapOf(
                    "stockRate" to String.format(
                        "%.1f%%",
                        (get("inStock")!! * 100.0 / get("total")!!)
                    ),
                    "needsAttention" to (get("lowStock")!! + get("outOfStock")!!),
                    "criticalCount" to get("outOfStock")
                )
            }

            mapOf(
                "users" to usersSummary,
                "orders" to ordersSummary,
                "revenue" to revenueSummary,
                "products" to productsSummary,
                "generatedAt" to LocalDateTime.now()
            )
        }

        @Suppress("UNCHECKED_CAST")
        val usersSummary = dashboardSummary["users"] as Map<String, Any>
        assertTrue((usersSummary["activeRate"] as String).contains("%"))

        @Suppress("UNCHECKED_CAST")
        val revenueSummary = dashboardSummary["revenue"] as Map<String, Any>
        assertNotNull(revenueSummary["avgDailyRevenue"])

        println("대시보드 요약: $dashboardSummary")
    }
}
