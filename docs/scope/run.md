# run - 객체 설정과 결과 계산을 결합한 함수

## 개요

`run`은 객체의 속성을 설정하거나 여러 작업을 수행한 후 결과를 반환하는 스코프 함수입니다. `apply`와 `let`의 특징을 결합한 형태로, 객체 컨텍스트 내에서 작업하면서 마지막 표현식을 반환합니다.

## 핵심 특징

- **컨텍스트 객체**: `this` (생략 가능)
- **반환값**: 람다의 마지막 표현식 결과
- **주 용도**: 객체 설정 + 결과 계산
- **의미**: "이 객체로 실행(run)하고 결과를 반환하라"

## 기본 문법

```kotlin
// 확장 함수로 사용
val result = someObject.run {
    // this를 사용 (생략 가능)
    property1 = value1
    computeResult()  // 마지막 표현식이 반환됨
}

// 독립 함수로 사용
val result = run {
    val temp = computation()
    processTemp(temp)
}
```

## run의 두 가지 형태

### 1. 확장 함수 run (더 자주 사용)

```kotlin
val result = user.run {
    username = "john"
    email = "john@example.com"
    username  // 반환
}
```

### 2. 독립 함수 run

```kotlin
val result = run {
    val x = 10
    val y = 20
    x + y  // 30 반환
}
```

## 언제 사용하는가?

1. **객체를 설정하고 계산 결과를 반환할 때**
   - apply처럼 설정하지만 객체가 아닌 다른 값 반환

2. **여러 작업을 수행하고 최종 결과만 필요할 때**
   - 중간 과정은 숨기고 결과만 노출

3. **nullable 객체를 안전하게 처리하며 변환할 때**
   - `?.run { }` 패턴

4. **지역 스코프에서 복잡한 계산을 수행할 때**
   - 임시 변수를 외부에 노출하지 않음

## 실무 예제

### 1. 객체 설정 후 계산 결과 반환

```kotlin
data class ShoppingCart(
    var items: MutableList<Item> = mutableListOf(),
    var discount: BigDecimal = BigDecimal.ZERO
)

val totalPrice = cart.run {
    items.add(Item("노트북", BigDecimal("1500000")))
    items.add(Item("마우스", BigDecimal("50000")))
    discount = BigDecimal("0.1")

    // 최종 금액 계산 후 반환
    val subtotal = items.sumOf { it.price }
    subtotal * (BigDecimal.ONE - discount)
}
```

### 2. 여러 설정 후 검증 결과 반환

```kotlin
val isValid = user.run {
    username = input.username.trim()
    email = input.email.lowercase()
    age = input.age

    // 검증 결과 반환
    username.length >= 3 &&
    email.contains("@") &&
    age >= 18
}
```

### 3. Nullable 객체 안전 처리

```kotlin
val userName = user?.run {
    "${firstName} ${lastName}"
} ?: "Guest"

val result = repository.findById(id)?.run {
    UserDTO(
        id = this.id,
        name = "${firstName} ${lastName}",
        email = email
    )
}
```

### 4. 복잡한 초기화와 계산

```kotlin
val config = run {
    val env = System.getenv("ENV") ?: "development"
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    Config(
        environment = env,
        port = port,
        database = if (env == "production") {
            "postgresql://prod-db:5432"
        } else {
            "postgresql://localhost:5432"
        }
    )
}
```

### 5. 데이터베이스 트랜잭션

```kotlin
fun transferMoney(fromId: Long, toId: Long, amount: BigDecimal): Boolean {
    return transaction {
        val from = findAccount(fromId) ?: return@transaction false
        val to = findAccount(toId) ?: return@transaction false

        from.run {
            if (balance < amount) return@transaction false
            balance -= amount
            save()
        }

        to.run {
            balance += amount
            save()
        }

        true
    }
}
```

### 6. 파일 처리

```kotlin
val lines = File("config.txt").run {
    if (!exists()) createNewFile()
    readLines()
        .filter { it.isNotBlank() }
        .map { it.trim() }
}
```

### 7. 빌더 패턴 대체 (결과 반환)

```kotlin
val report = Report().run {
    title = "Monthly Sales"
    period = LocalDate.now().month.toString()
    data = fetchSalesData()

    // 리포트 생성 후 파일 경로 반환
    val fileName = "report_${period}.pdf"
    generatePDF(fileName)
    fileName
}
```

### 8. 조건부 처리

```kotlin
val message = when (status) {
    Status.PENDING -> "주문 대기 중"
    Status.CONFIRMED -> order.run {
        confirmedAt = LocalDateTime.now()
        "주문이 확인되었습니다: ${id}"
    }
    Status.SHIPPED -> order.run {
        shippedAt = LocalDateTime.now()
        trackingNumber = generateTrackingNumber()
        "배송이 시작되었습니다: ${trackingNumber}"
    }
    Status.DELIVERED -> "배송 완료"
}
```

### 9. 체이닝에서 중간 변환

```kotlin
val result = inputString
    .trim()
    .lowercase()
    .run {
        // 복잡한 처리
        val words = split(" ")
        val filtered = words.filter { it.length > 3 }
        filtered.joinToString("-")
    }
    .uppercase()
```

### 10. API 응답 변환

```kotlin
fun getUser(id: Long): ApiResponse<UserDTO> {
    return userRepository.findById(id)?.run {
        ApiResponse.success(
            UserDTO(
                id = id,
                name = "${firstName} ${lastName}",
                email = email,
                age = Period.between(birthDate, LocalDate.now()).years
            )
        )
    } ?: ApiResponse.error("User not found")
}
```

## run vs let vs apply

```kotlin
val user = User()

// apply: 객체 설정 후 객체 반환
val user1: User = user.apply {
    username = "john"
    email = "john@example.com"
}

// run: 객체 설정 후 다른 값 반환
val isValid: Boolean = user.run {
    username = "john"
    email = "john@example.com"
    email.contains("@")
}

// let: 객체를 변환
val email: String = user.let {
    it.username = "john"
    it.email
}
```

## 독립 함수 run의 활용

### 1. 복잡한 표현식 캡슐화

```kotlin
val result = run {
    val a = computeA()
    val b = computeB()
    val c = computeC()
    (a + b) * c
}
```

### 2. 지역 변수 스코프 제한

```kotlin
fun processData(input: String) {
    val cleaned = run {
        val trimmed = input.trim()
        val lowercase = trimmed.lowercase()
        val normalized = lowercase.replace(Regex("\\s+"), " ")
        normalized
    }

    save(cleaned)
    // trimmed, lowercase, normalized는 여기서 접근 불가
}
```

### 3. 초기화 블록

```kotlin
class DatabaseConfig {
    val url: String
    val username: String
    val password: String

    init {
        run {
            val env = System.getenv("ENV")
            val isDev = env == "development"

            url = if (isDev) "localhost" else "prod-server"
            username = if (isDev) "dev" else "admin"
            password = if (isDev) "dev123" else System.getenv("DB_PASSWORD")
        }
    }
}
```

## Nullable 체크와 run

```kotlin
// 기본 패턴
val result = nullableObject?.run {
    // null이 아닐 때만 실행
    performOperation()
}

// 실무 예제
val displayName = user?.run {
    "${firstName} ${lastName}".takeIf { it.isNotBlank() }
} ?: "Anonymous"

// 중첩된 nullable
val city = user?.address?.run {
    "$street, $city, $state $zipCode"
}
```

## 베스트 프랙티스

### ✅ 권장

1. **설정 + 계산 결과 반환**
   ```kotlin
   val total = cart.run {
       addItem(item)
       calculateTotal()
   }
   ```

2. **Nullable 객체 변환**
   ```kotlin
   val dto = user?.run {
       UserDTO(username, email)
   }
   ```

3. **복잡한 표현식 캡슐화**
   ```kotlin
   val result = run {
       val x = compute1()
       val y = compute2()
       x + y
   }
   ```

4. **검증 후 결과 반환**
   ```kotlin
   val isValid = user.run {
       username.isNotEmpty() && email.contains("@")
   }
   ```

### ❌ 비권장

1. **단순히 객체만 반환 (apply 사용 권장)**
   ```kotlin
   // Bad
   val user = User().run {
       username = "john"
       this  // 객체만 반환
   }

   // Good
   val user = User().apply {
       username = "john"
   }
   ```

2. **단순 값 변환 (let 사용 권장)**
   ```kotlin
   // Bad
   val name = user.run { it.username }

   // Good
   val name = user.let { it.username }
   ```

## 실전 팁

### 1. with vs run

```kotlin
// with: 파라미터로 객체 전달
val result = with(user) {
    username = "john"
    email.length
}

// run: 확장 함수
val result = user.run {
    username = "john"
    email.length
}

// nullable 처리
val result = user?.run {  // OK
    email.length
}

// with는 nullable 처리 불가
val result = with(user) {  // user가 null이면 NPE
    email.length
}
```

### 2. return@ 레이블 사용

```kotlin
fun process(): String {
    return list.firstOrNull { it > 10 }?.run {
        if (this < 20) {
            return@run "Small"
        }
        "Large"
    } ?: "None"
}
```

### 3. 중첩 run 피하기

```kotlin
// 나쁜 예
val result = user.run {
    address.run {
        city.run {
            uppercase()
        }
    }
}

// 좋은 예
val result = user.address.city.uppercase()

// 또는
val result = user.run {
    address.city.uppercase()
}
```

## 요약

`run`은 객체 컨텍스트 내에서 작업을 수행하면서 계산 결과를 반환할 때 사용하는 강력한 함수입니다. `apply`(객체 반환)와 `let`(값 변환)의 중간 형태로, 객체를 설정하면서 동시에 결과값이 필요한 경우에 적합합니다. 특히 `?.run { }` 패턴은 nullable 객체를 안전하게 처리하면서 변환하는 우아한 방법입니다.

## 관련 문서

- [also.md](also.md) - 부수 효과 작업
- [apply.md](apply.md) - 객체 초기화
- [let.md](let.md) - 값 변환
- [with.md](with.md) - 객체 컨텍스트 작업