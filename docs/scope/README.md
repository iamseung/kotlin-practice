# Kotlin Scope Functions 완벽 가이드

Kotlin의 5가지 스코프 함수(let, run, with, apply, also)에 대한 실무 중심의 종합 가이드입니다.

## 📚 문서 목록

- **[let.md](let.md)** - 값 변환 및 null 안전성
- **[run.md](run.md)** - 객체 설정과 결과 계산
- **[with.md](with.md)** - 객체 컨텍스트에서 여러 작업 수행
- **[apply.md](apply.md)** - 객체 초기화 및 설정
- **[also.md](also.md)** - 부수 효과(Side-Effect) 작업

## 🎯 빠른 비교표

| 함수 | 객체 참조 | 반환값 | 확장 함수 | 주 용도 | 대표 사용 사례 |
|------|----------|--------|-----------|---------|----------------|
| **let** | `it` | 람다 결과 | ✓ | 값 변환, null 체크 | `user?.let { sendEmail(it.email) }` |
| **run** | `this` | 람다 결과 | ✓ | 설정 + 결과 반환 | `user.run { "${firstName} ${lastName}" }` |
| **with** | `this` | 람다 결과 | ✗ | 객체로 여러 작업 | `with(canvas) { drawCircle(...); drawLine(...) }` |
| **apply** | `this` | 객체 자체 | ✓ | 객체 초기화 | `User().apply { id = 1; name = "john" }` |
| **also** | `it` | 객체 자체 | ✓ | 부수 효과 | `user.also { logger.info("Created: ${it.name}") }` |

## 🔍 어떤 함수를 선택할까?

### 의사결정 플로우차트

```
객체를 변환하거나 결과값이 필요한가?
├─ YES
│  ├─ null 체크가 필요한가?
│  │  ├─ YES → let
│  │  └─ NO
│  │     ├─ 객체가 파라미터인가? → with
│  │     └─ 객체가 리시버인가? → run
│  │
└─ NO (객체 자체를 반환)
   ├─ 객체 속성을 설정하는가?
   │  ├─ YES → apply
   │  └─ NO → also (부수 효과)
```

### 상황별 추천

#### 1. 객체 생성 및 초기화
```kotlin
val user = User().apply {
    id = 1
    username = "john"
    email = "john@example.com"
}
```
**선택**: `apply` - 객체를 설정하고 그 객체를 반환

#### 2. Nullable 객체 처리
```kotlin
user?.let { u ->
    sendEmail(u.email)
}
```
**선택**: `let` - null이 아닐 때만 실행

#### 3. 로깅/감사 추가
```kotlin
user.also {
    logger.info("User created: ${it.username}")
}
```
**선택**: `also` - 부수 효과를 수행하면서 객체 반환

#### 4. 객체 설정 후 다른 값 반환
```kotlin
val isValid = user.run {
    username = input.username
    email = input.email
    email.contains("@")
}
```
**선택**: `run` - 설정 후 계산 결과 반환

#### 5. 여러 속성/메서드 연속 호출
```kotlin
val summary = with(order) {
    val total = items.sumOf { it.price }
    val count = items.size
    "주문 ${id}: ${count}개 상품, 총 ${total}원"
}
```
**선택**: `with` - 객체의 여러 멤버에 접근

## 💡 실무 패턴

### 패턴 1: apply + also (초기화 + 부수효과)

```kotlin
fun createUser(username: String): User {
    return User()
        .apply {
            // 객체 설정
            this.username = username
            email = "${username}@example.com"
            createdAt = LocalDateTime.now()
        }
        .also {
            // 부수 효과
            logger.info("User created: ${it.username}")
            eventBus.publish(UserCreatedEvent(it))
        }
}
```

### 패턴 2: let + Elvis (null 처리 + 기본값)

```kotlin
val displayName = user?.let {
    "${it.firstName} ${it.lastName}"
} ?: "Guest"
```

### 패턴 3: run (설정 + 검증)

```kotlin
val isValid = user.run {
    username = input.username.trim()
    email = input.email.lowercase()

    // 검증 결과 반환
    username.length >= 3 && email.contains("@")
}
```

### 패턴 4: with (여러 데이터 처리)

```kotlin
val report = with(order) {
    val total = items.sumOf { it.price * it.quantity.toBigDecimal() }
    val discount = calculateDiscount(total)
    val tax = calculateTax(total - discount)

    OrderReport(id, total, discount, tax)
}
```

### 패턴 5: also (파이프라인 디버깅)

```kotlin
val result = data
    .filter { it.isValid }
    .also { println("After filter: $it") }
    .map { it.transform() }
    .also { println("After map: $it") }
    .sum()
```

## ⚠️ 흔한 실수

### 1. Nullable 객체에 with 사용
```kotlin
// ❌ Bad - NPE 가능
val name = with(nullableUser) {
    "$firstName $lastName"
}

// ✅ Good - null-safe
val name = nullableUser?.run {
    "$firstName $lastName"
}
```

### 2. 부수 효과에 apply 사용
```kotlin
// ❌ Bad - 의도가 불명확
user.apply {
    logger.info("User: $username")
}

// ✅ Good - 의도가 명확
user.also {
    logger.info("User: ${it.username}")
}
```

### 3. 객체 설정에 also 사용
```kotlin
// ❌ Bad
user.also {
    it.username = "john"
    it.email = "john@example.com"
}

// ✅ Good
user.apply {
    username = "john"
    email = "john@example.com"
}
```

### 4. 단순 변환에 run 사용
```kotlin
// ❌ Bad
val upper = str.run { uppercase() }

// ✅ Good
val upper = str.uppercase()
```

## 🎓 학습 순서 추천

1. **apply** - 가장 직관적이고 자주 사용
2. **also** - apply와 유사하지만 부수 효과용
3. **let** - nullable 처리의 핵심
4. **run** - apply + let의 조합 이해
5. **with** - run과 유사하지만 파라미터 형태

## 📖 추가 학습 자료

### 테스트 코드
실제 동작하는 예제는 각 테스트 파일을 참고하세요:

- `src/test/kotlin/com/example/kotlin/single_test/LetTest.kt`
- `src/test/kotlin/com/example/kotlin/single_test/RunTest.kt`
- `src/test/kotlin/com/example/kotlin/single_test/WithTest.kt`
- `src/test/kotlin/com/example/kotlin/single_test/ApplyTest.kt`
- `src/test/kotlin/com/example/kotlin/single_test/AlsoTest.kt`

### 공식 문서
- [Kotlin 공식 문서 - Scope Functions](https://kotlinlang.org/docs/scope-functions.html)

## 🚀 실전 팁

1. **일관성 유지**: 팀 내에서 사용 규칙을 정하고 일관되게 사용
2. **명시적 이름**: `it` 대신 의미 있는 이름 사용 (`user`, `item` 등)
3. **중첩 최소화**: 2단계 이상 중첩은 가독성 저하
4. **적절한 선택**: 모든 상황에 scope function이 필요한 것은 아님
5. **코드 리뷰**: 동료와 함께 적절한 사용 여부 검토

## 📝 요약

Kotlin의 스코프 함수는 코드를 더 간결하고 읽기 쉽게 만들어주는 강력한 도구입니다. 각 함수의 특성을 이해하고 적절히 활용하면 Kotlin다운 코드를 작성할 수 있습니다.

**핵심은**:
- `let` - 값 변환 & null 체크
- `run` - 설정 + 결과
- `with` - 여러 작업 수행
- `apply` - 객체 초기화
- `also` - 부수 효과

Happy Kotlin Coding! 🎉