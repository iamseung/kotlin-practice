# with - 객체 컨텍스트에서 여러 작업을 수행하는 함수

## 개요

`with`는 특정 객체의 컨텍스트 내에서 여러 작업을 수행하고 결과를 반환하는 스코프 함수입니다. 객체의 여러 함수나 속성을 연속으로 호출하거나 데이터를 처리할 때 유용합니다.

## 핵심 특징

- **컨텍스트 객체**: `this` (생략 가능)
- **반환값**: 람다의 마지막 표현식 결과
- **주 용도**: 객체의 함수 호출, 데이터 접근 및 변환
- **의미**: "이 객체로(with) 다음 작업을 수행하라"
- **주의**: non-null 객체에만 사용 (nullable이면 `?.run` 사용)

## 기본 문법

```kotlin
val result = with(someObject) {
    // this로 객체에 접근 (생략 가능)
    property1
    method()
    computeResult()  // 마지막 표현식이 반환됨
}
```

## 언제 사용하는가?

1. **특정 객체의 여러 함수나 속성을 연속으로 호출할 때**
   - 반복적인 객체 참조 제거

2. **객체의 데이터를 사용해서 계산이나 변환을 수행할 때**
   - 데이터 처리 및 집계

3. **"이 객체로(with) 다음 작업을 수행하라"는 의미가 명확할 때**
   - 코드의 의도를 명확하게 표현

## with vs run

```kotlin
// with: 파라미터로 객체 전달
val result = with(user) {
    "$firstName $lastName"
}

// run: 확장 함수
val result = user.run {
    "$firstName $lastName"
}

// 차이점: nullable 처리
val user: User? = getUser()

// with는 nullable 처리 불가
val name1 = with(user) { ... }  // user가 null이면 NPE

// run은 nullable 처리 가능
val name2 = user?.run { ... }   // null-safe
```

## 실무 예제

### 1. 객체 속성에 연속 접근

```kotlin
data class Product(
    val name: String,
    val price: BigDecimal,
    val quantity: Int,
    val category: String
)

val product = Product("노트북", BigDecimal("1500000"), 10, "전자기기")

val description = with(product) {
    "$name은(는) $category 카테고리의 상품입니다. " +
    "가격: ${price}원, 재고: ${quantity}개"
}
```

### 2. 복잡한 계산 수행

```kotlin
data class Order(
    val items: List<OrderItem>,
    val shippingAddress: Address
)

val orderSummary = with(order) {
    val subtotal = items.sumOf { it.price * it.quantity.toBigDecimal() }
    val discount = items.sumOf { item ->
        val itemTotal = item.price * item.quantity.toBigDecimal()
        itemTotal * item.discount
    }
    val finalAmount = subtotal - discount
    val shippingFee = if (finalAmount >= BigDecimal("1000000")) {
        BigDecimal.ZERO
    } else {
        BigDecimal("3000")
    }

    mapOf(
        "subtotal" to subtotal,
        "discount" to discount,
        "shippingFee" to shippingFee,
        "total" to finalAmount + shippingFee
    )
}
```

### 3. StringBuilder로 리포트 생성

```kotlin
val employee = Employee(
    id = 1,
    name = "김철수",
    department = "개발팀",
    salary = BigDecimal("7000000"),
    hireDate = LocalDate.of(2020, 3, 15)
)

val report = with(StringBuilder()) {
    appendLine("=".repeat(50))
    appendLine("직원 정보 상세 리포트")
    appendLine("=".repeat(50))
    appendLine()

    appendLine("[기본 정보]")
    appendLine("이름: ${employee.name}")
    appendLine("부서: ${employee.department}")
    appendLine()

    appendLine("[급여 정보]")
    val yearlySalary = employee.salary * BigDecimal("12")
    appendLine("월급: ${employee.salary}원")
    appendLine("연봉: ${yearlySalary}원")

    toString()
}
```

### 4. 사각형의 여러 속성 계산

```kotlin
data class Rectangle(val topLeft: Point, val bottomRight: Point)

val properties = with(rectangle) {
    val width = bottomRight.x - topLeft.x
    val height = topLeft.y - bottomRight.y
    val area = width * height
    val perimeter = 2 * (width + height)
    val diagonal = sqrt(width * width + height * height)
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
```

### 5. 리스트 통계 계산

```kotlin
val products = listOf(
    Product("상품A", BigDecimal("10000"), 100, "카테고리1"),
    Product("상품B", BigDecimal("20000"), 50, "카테고리1"),
    Product("상품C", BigDecimal("15000"), 75, "카테고리2")
)

val statistics = with(products) {
    val totalProducts = size
    val totalValue = sumOf { it.price * it.quantity.toBigDecimal() }
    val avgPrice = sumOf { it.price }.divide(size.toBigDecimal(), 2, RoundingMode.HALF_UP)
    val maxPrice = maxOf { it.price }
    val minPrice = minOf { it.price }

    mapOf(
        "totalProducts" to totalProducts,
        "totalValue" to totalValue,
        "averagePrice" to avgPrice,
        "maxPrice" to maxPrice,
        "minPrice" to minPrice
    )
}
```

### 6. Map 데이터 처리 및 변환

```kotlin
val userPreferences = mapOf(
    "theme" to "dark",
    "language" to "ko",
    "fontSize" to 14,
    "notifications" to mapOf(
        "email" to true,
        "push" to false,
        "sms" to true
    )
)

val summary = with(userPreferences) {
    val theme = get("theme") as? String ?: "light"
    val language = get("language") as? String ?: "en"
    val fontSize = get("fontSize") as? Int ?: 12

    @Suppress("UNCHECKED_CAST")
    val notifications = get("notifications") as? Map<String, Boolean> ?: emptyMap()
    val enabledNotifications = notifications.filter { it.value }.keys

    """
        사용자 설정 요약:
        - 테마: $theme
        - 언어: $language
        - 글꼴 크기: ${fontSize}px
        - 활성화된 알림: ${enabledNotifications.joinToString(", ")}
    """.trimIndent()
}
```

### 7. 중첩된 with 사용

```kotlin
val invoice = with(order) {
    val customerInfo = "고객 ID: $customerId"

    val itemsDetail = with(items) {
        joinToString("\n") { item ->
            with(item) {
                val itemTotal = price * quantity.toBigDecimal()
                val discountAmount = itemTotal * discount
                val finalPrice = itemTotal - discountAmount
                "- $productName x$quantity = ${itemTotal}원 (할인: ${discountAmount}원)"
            }
        }
    }

    val addressInfo = with(shippingAddress) {
        "$country $state $city $street ($zipCode)"
    }

    """
        ====== 주문서 ======
        주문 번호: $id
        $customerInfo

        [주문 상품]
        $itemsDetail

        [배송지]
        $addressInfo
        ===================
    """.trimIndent()
}
```

### 8. 다른 scope function과 조합

```kotlin
val employees = listOf(
    Employee(1, "김철수", "개발팀", BigDecimal("7000000")),
    Employee(2, "이영희", "개발팀", BigDecimal("5000000")),
    Employee(3, "박민수", "디자인팀", BigDecimal("6000000"))
)

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
```

### 9. 데이터 검증

```kotlin
val validationResult = with(order) {
    val errors = mutableListOf<String>()

    if (items.isEmpty()) {
        errors.add("주문 상품이 없습니다")
    }

    items.forEach { item ->
        if (item.price <= BigDecimal.ZERO) {
            errors.add("${item.productName}의 가격이 유효하지 않습니다")
        }
        if (item.quantity <= 0) {
            errors.add("${item.productName}의 수량이 유효하지 않습니다")
        }
    }

    with(shippingAddress) {
        if (street.isBlank()) errors.add("주소가 입력되지 않았습니다")
        if (city.isBlank()) errors.add("도시가 입력되지 않았습니다")
    }

    mapOf(
        "isValid" to errors.isEmpty(),
        "errors" to errors
    )
}
```

### 10. 대시보드 데이터 집계

```kotlin
val metricsData = mapOf(
    "users" to mapOf("total" to 150000, "active" to 45000),
    "orders" to mapOf("total" to 250000, "completed" to 248000),
    "revenue" to mapOf("total" to BigDecimal("50000000000"))
)

val dashboardSummary = with(metricsData) {
    @Suppress("UNCHECKED_CAST")
    val users = get("users") as Map<String, Int>
    @Suppress("UNCHECKED_CAST")
    val orders = get("orders") as Map<String, Int>

    val usersSummary = with(users) {
        mapOf(
            "activeRate" to String.format(
                "%.1f%%",
                (get("active")!! * 100.0 / get("total")!!)
            )
        )
    }

    val ordersSummary = with(orders) {
        mapOf(
            "completionRate" to String.format(
                "%.1f%%",
                (get("completed")!! * 100.0 / get("total")!!)
            )
        )
    }

    mapOf(
        "users" to usersSummary,
        "orders" to ordersSummary
    )
}
```

## 베스트 프랙티스

### ✅ 권장

1. **객체의 여러 속성 접근**
   ```kotlin
   val info = with(user) {
       "$name ($email) - ${role}"
   }
   ```

2. **복잡한 계산**
   ```kotlin
   val result = with(data) {
       val sum = items.sum()
       val avg = sum / items.size
       avg * factor
   }
   ```

3. **StringBuilder 사용**
   ```kotlin
   val text = with(StringBuilder()) {
       append("Hello")
       append(" ")
       append("World")
       toString()
   }
   ```

4. **여러 객체 속성으로 계산**
   ```kotlin
   val total = with(cart) {
       items.sumOf { it.price * it.quantity }
   }
   ```

### ❌ 비권장

1. **Nullable 객체 (run 사용 권장)**
   ```kotlin
   // Bad
   val name = with(nullableUser) { ... }  // NPE 가능

   // Good
   val name = nullableUser?.run { ... }
   ```

2. **객체 설정만 하는 경우 (apply 사용 권장)**
   ```kotlin
   // Bad
   with(user) {
       username = "john"
       email = "john@example.com"
   }

   // Good
   user.apply {
       username = "john"
       email = "john@example.com"
   }
   ```

3. **단순 값 변환 (let 사용 권장)**
   ```kotlin
   // Bad
   val upper = with(str) { uppercase() }

   // Good
   val upper = str.let { it.uppercase() }
   // 또는
   val upper = str.uppercase()
   ```

## with의 장점

1. **코드 간결성**
   - 반복되는 객체 참조 제거
   - 코드가 더 읽기 쉬워짐

2. **의도 명확성**
   - "이 객체로 작업한다"는 의도가 명확
   - 스코프가 명확히 구분됨

3. **this 생략**
   - 객체의 멤버에 직접 접근하는 것처럼 자연스러움

## 실전 팁

### 1. 긴 연산을 with로 캡슐화

```kotlin
// 복잡한 계산을 명확히 구분
val finalPrice = with(order) {
    val baseTotal = items.sumOf { it.price * it.quantity.toBigDecimal() }
    val discountAmount = calculateDiscount(baseTotal)
    val taxAmount = calculateTax(baseTotal - discountAmount)
    val shippingFee = calculateShipping(baseTotal)

    baseTotal - discountAmount + taxAmount + shippingFee
}
```

### 2. 여러 객체의 데이터 조합

```kotlin
fun createReport(user: User, stats: Statistics) = with(user) {
    with(stats) {
        Report(
            userName = this@with.name,
            totalOrders = orderCount,
            totalRevenue = revenue,
            avgOrderValue = revenue / orderCount.toBigDecimal()
        )
    }
}
```

### 3. Canvas/Graphics 작업

```kotlin
with(canvas) {
    drawColor(Color.WHITE)
    drawCircle(100f, 100f, 50f, paint)
    drawLine(0f, 0f, 200f, 200f, paint)
    drawText("Hello", 50f, 50f, textPaint)
}
```

## 요약

`with`는 특정 객체의 컨텍스트 내에서 여러 작업을 수행할 때 사용하는 함수입니다. 객체의 여러 속성이나 함수에 반복적으로 접근해야 할 때 코드를 간결하게 만들어줍니다. 단, nullable 객체에는 사용할 수 없으므로 그런 경우에는 `?.run`을 사용해야 합니다.

## 5가지 스코프 함수 비교표

| 함수 | 객체 참조 | 반환값 | 확장 함수 | 주 용도 |
|------|----------|--------|-----------|---------|
| **let** | it | 람다 결과 | ✓ | 값 변환, null 체크 |
| **run** | this | 람다 결과 | ✓ | 설정 + 결과 반환 |
| **with** | this | 람다 결과 | ✗ | 객체로 여러 작업 |
| **apply** | this | 객체 자체 | ✓ | 객체 초기화 |
| **also** | it | 객체 자체 | ✓ | 부수 효과 |

## 관련 문서

- [also.md](also.md) - 부수 효과 작업
- [apply.md](apply.md) - 객체 초기화
- [let.md](let.md) - 값 변환
- [run.md](run.md) - 객체 설정 + 결과 반환