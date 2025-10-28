# also - 부수 효과(Side-Effect) 작업에 특화된 함수

## 개요

`also`는 객체에 대한 부수 효과 작업을 수행하는 스코프 함수입니다. 주로 로깅, 검증, 디버깅, 이벤트 발행 등의 추가 작업을 수행하면서 원본 객체를 반환할 때 사용합니다.

## 핵심 특징

- **컨텍스트 객체**: `it` (명시적으로 이름 변경 가능)
- **반환값**: 컨텍스트 객체 자체
- **주 용도**: 로깅, 검증, 디버깅 등의 부수 효과
- **의미**: "이것을 하고 또한(also) 저것도 해라"

## 기본 문법

```kotlin
val result = someObject.also { obj ->
    // obj를 사용한 부수 효과 작업
    // 원본 someObject가 반환됨
}
```

## 언제 사용하는가?

1. **객체의 속성을 변경하지 않고 추가 작업을 수행할 때**
   - 로깅, 감사(audit), 메트릭 수집

2. **객체를 컬렉션에 추가하면서 동시에 다른 작업을 할 때**
   - 생성과 동시에 리스트에 추가하고 로깅

3. **디버깅을 위해 중간 값을 출력할 때**
   - 체이닝 중간에 값 확인

4. **객체 생성/수정 시 이벤트를 발행할 때**
   - 도메인 이벤트 발행, 알림 전송

## apply vs also 비교

| 특징 | apply | also |
|-----|-------|------|
| 컨텍스트 객체 | `this` | `it` |
| 주 용도 | 객체 초기화/설정 | 부수 효과 작업 |
| 느낌 | 내부 작업 | 외부 작업 |
| 사용 예 | 속성 설정 | 로깅, 검증 |

```kotlin
// apply: 객체 설정
val user = User().apply {
    id = 1
    username = "john"
    email = "john@example.com"
}

// also: 부수 효과
val user = User().apply {
    id = 1
    username = "john"
}.also { user ->
    log("User created: ${user.username}")
}
```

## 실무 예제

### 1. 로깅과 함께 객체 반환

```kotlin
fun createUser(username: String, email: String): User {
    return User().apply {
        this.username = username
        this.email = email
    }.also {
        logger.info("User created: ${it.username}")
    }
}
```

### 2. 컬렉션에 추가하면서 로깅

```kotlin
val users = mutableListOf<User>()

fun createUser(username: String): User {
    return User().apply {
        id = (users.size + 1).toLong()
        this.username = username
    }.also { newUser ->
        users.add(newUser)
        log("Created user: ${newUser.username} (ID: ${newUser.id})")
    }
}
```

### 3. 감사 로그(Audit Log) 기록

```kotlin
fun updateUserEmail(user: User, newEmail: String): User {
    return user.apply {
        email = newEmail
        lastModifiedAt = LocalDateTime.now()
    }.also {
        auditLog.record(
            action = "USER_EMAIL_UPDATED",
            userId = it.id,
            details = "Email changed to ${it.email}"
        )
    }
}
```

### 4. 디버깅 - 파이프라인 중간 값 확인

```kotlin
val result = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    .also { println("Initial: $it") }
    .filter { it % 2 == 0 }
    .also { println("After filter: $it") }
    .map { it * 2 }
    .also { println("After map: $it") }
    .sum()
    .also { println("Final sum: $it") }
```

### 5. 이벤트 발행

```kotlin
fun createOrder(userId: Long, amount: BigDecimal): Order {
    return Order(
        id = generateId(),
        userId = userId,
        amount = amount,
        status = "PENDING"
    ).also { order ->
        eventBus.publish(
            OrderCreatedEvent(
                orderId = order.id,
                userId = order.userId,
                amount = order.amount
            )
        )
    }
}
```

### 6. 캐시 시스템

```kotlin
fun putCache(key: String, value: User): User {
    return value.also {
        cache[key] = CacheEntry(
            key = key,
            value = it,
            cachedAt = LocalDateTime.now()
        )
        cacheStats["puts"]++
    }
}
```

### 7. 알림 발송

```kotlin
fun registerUser(username: String, email: String): User {
    return User().apply {
        this.username = username
        this.email = email
    }.also { user ->
        notificationService.sendWelcomeEmail(user.email)
        notificationService.sendSMS(user.phone, "회원가입 완료")
        analyticsService.track("user_registered", user.id)
    }
}
```

## return object.also { } 패턴

이 패턴은 실무에서 매우 자주 사용됩니다:

```kotlin
fun processPayment(amount: BigDecimal): Payment {
    return Payment()
        .apply {
            // 객체 초기화
            id = "PAY-${System.currentTimeMillis()}"
            this.amount = amount
            status = "COMPLETED"
        }
        .also { payment ->
            // 부수 효과
            auditLog.record("Payment processed: ${payment.id}")
            notificationService.send("Payment completed")
            metricsService.record("payment_completed", payment.amount)
        }
}
```

**의도**: "객체는 반환하되, 반환하기 전에 관찰/기록/알림 등의 부수 효과를 수행"

## 체이닝 - 여러 부수 효과 수행

```kotlin
fun createOrder(userId: Long, amount: BigDecimal): Order {
    return Order().apply {
        this.userId = userId
        this.amount = amount
    }
        .also { order ->
            // 로그 기록
            logger.info("Order created: ${order.id}")
        }
        .also { order ->
            // 알림 발송
            notificationService.notify(order.userId, "주문이 생성되었습니다")
        }
        .also { order ->
            // 분석 데이터 수집
            analytics.track("order_created", order.id)
        }
}
```

## apply와 also를 함께 사용하는 이유

```kotlin
// 나쁜 예: 모든 것을 apply 안에
fun processPaymentBad(amount: BigDecimal): Payment {
    return Payment().apply {
        id = generateId()
        this.amount = amount
        status = "COMPLETED"

        // apply는 객체 설정용인데 부수 효과까지 - 혼란스러움!
        auditLog.add("Payment processed: $id")
        notifications.send("Payment completed")
    }
}

// 좋은 예: apply는 설정, also는 부수 효과
fun processPaymentGood(amount: BigDecimal): Payment {
    return Payment()
        .apply {
            // 명확: 객체 설정만!
            id = generateId()
            this.amount = amount
            status = "COMPLETED"
        }
        .also { payment ->
            // 명확: 부수 효과만!
            auditLog.add("Payment processed: ${payment.id}")
            notifications.send("Payment completed")
        }
}
```

## 베스트 프랙티스

### ✅ 권장

1. **부수 효과 작업에 사용**
   ```kotlin
   user.also { logger.info("User: ${it.username}") }
   ```

2. **체이닝에서 디버깅**
   ```kotlin
   data.filter { ... }
       .also { println("Filtered: $it") }
       .map { ... }
   ```

3. **이벤트/알림 발행**
   ```kotlin
   order.also { eventBus.publish(OrderCreatedEvent(it)) }
   ```

### ❌ 비권장

1. **객체 속성 설정 (apply 사용 권장)**
   ```kotlin
   // Bad
   user.also {
       it.username = "john"
       it.email = "john@example.com"
   }

   // Good
   user.apply {
       username = "john"
       email = "john@example.com"
   }
   ```

2. **결과 변환 (let/map 사용 권장)**
   ```kotlin
   // Bad
   val name = user.also { it.username.uppercase() }

   // Good
   val name = user.let { it.username.uppercase() }
   ```

## 요약

`also`는 "이 객체로 작업하고, **또한** 이런 일도 해라"라는 의미로, 객체를 반환하면서 부수 효과를 수행할 때 사용하는 강력한 도구입니다. `apply`와 함께 사용하면 객체 초기화와 부수 효과를 명확히 분리할 수 있어 코드의 가독성과 유지보수성이 크게 향상됩니다.

## 관련 문서

- [apply.md](apply.md) - 객체 초기화
- [let.md](let.md) - 값 변환
- [run.md](run.md) - 객체 설정 + 결과 반환
- [with.md](with.md) - 객체 컨텍스트 작업