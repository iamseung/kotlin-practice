# let - 값 변환 및 null 안전성에 특화된 함수

## 개요

`let`은 객체를 변환하거나 null이 아닌 경우에만 작업을 수행할 때 사용하는 스코프 함수입니다. 함수형 프로그래밍 스타일의 변환 작업과 null 안전성을 보장하는 데 특화되어 있습니다.

## 핵심 특징

- **컨텍스트 객체**: `it` (명시적으로 이름 변경 가능)
- **반환값**: 람다의 마지막 표현식 결과
- **주 용도**: 값 변환, null 안전성 체크
- **의미**: "이 값으로(let) 다음 작업을 수행하라"

## 기본 문법

```kotlin
val result = someObject.let { obj ->
    // obj를 사용한 변환 작업
    transformedValue  // 마지막 표현식이 반환됨
}
```

## 언제 사용하는가?

1. **값을 변환할 때**
   - 한 타입을 다른 타입으로 변환
   - 데이터 가공 및 처리

2. **nullable 객체를 안전하게 처리할 때**
   - `?.let { }` 패턴으로 null 체크
   - null이 아닐 때만 실행

3. **지역 변수의 스코프를 제한할 때**
   - 임시 변수를 let 블록 내로 제한

4. **메서드 체이닝 중간에 변환을 수행할 때**
   - 파이프라인에서 값 변환

## 실무 예제

### 1. 값 변환

```kotlin
data class User(val id: Long, val username: String, val email: String)
data class UserDTO(val username: String, val email: String)

// User → UserDTO 변환
val user = User(1, "john", "john@example.com")
val dto = user.let { UserDTO(it.username, it.email) }
```

### 2. Nullable 객체 안전하게 처리

```kotlin
// 기본 방법
val user: User? = findUser(id)
if (user != null) {
    sendEmail(user.email)
}

// let 사용 - 더 간결!
val user: User? = findUser(id)
user?.let { sendEmail(it.email) }

// 여러 nullable 속성 접근
user?.let {
    println("Name: ${it.username}")
    println("Email: ${it.email}")
    sendWelcomeEmail(it.email)
}
```

### 3. 변환 체이닝

```kotlin
val result = getUserId()
    .let { findUser(it) }           // Long → User?
    .let { it?.email }               // User? → String?
    .let { it?.lowercase() }         // String? → String?
    ?: "unknown@example.com"         // null일 경우 기본값
```

### 4. 지역 변수 스코프 제한

```kotlin
// 나쁜 예: 불필요한 변수가 전체 스코프에 남음
fun processOrder(order: Order) {
    val total = order.items.sumOf { it.price * it.quantity }
    val tax = total * 0.1
    val finalAmount = total + tax

    saveOrder(order, finalAmount)
    // total, tax는 더 이상 필요 없지만 스코프에 남아있음
}

// 좋은 예: let으로 스코프 제한
fun processOrder(order: Order) {
    val finalAmount = order.items
        .sumOf { it.price * it.quantity }
        .let { total ->
            val tax = total * 0.1
            total + tax
        }

    saveOrder(order, finalAmount)
}
```

### 5. 복잡한 표현식을 변수로 변환

```kotlin
// 긴 표현식을 읽기 쉽게
val discountedPrice = product.price
    .let { basePrice ->
        when {
            customer.isPremium -> basePrice * 0.8
            order.total > 100000 -> basePrice * 0.9
            else -> basePrice
        }
    }
```

### 6. 여러 연산을 순차적으로 수행

```kotlin
fun processUserData(userId: Long): String {
    return userId
        .let { findUser(it) }                    // DB에서 사용자 조회
        .let { user ->
            user?.copy(
                email = user.email.lowercase(),   // 이메일 정규화
                username = user.username.trim()   // 공백 제거
            )
        }
        .let { normalizedUser ->
            normalizedUser?.let {
                validateUser(it)                  // 검증
                saveUser(it)                      // 저장
                it.username
            } ?: "Unknown"
        }
}
```

### 7. Null 체크와 변환 결합

```kotlin
// Repository 계층
interface UserRepository {
    fun findById(id: Long): User?
}

// Service 계층
class UserService(private val repository: UserRepository) {
    fun getUserDTO(id: Long): UserDTO? {
        return repository.findById(id)?.let { user ->
            UserDTO(
                username = user.username,
                email = user.email,
                fullName = "${user.firstName} ${user.lastName}"
            )
        }
    }
}
```

### 8. Elvis 연산자와 함께 사용

```kotlin
fun getDisplayName(user: User?): String {
    return user?.let {
        "${it.firstName} ${it.lastName}"
    } ?: "Guest"
}

fun processOrder(orderId: Long): OrderResult {
    return findOrder(orderId)?.let { order ->
        validateOrder(order)
        processPayment(order)
        OrderResult.Success(order.id)
    } ?: OrderResult.NotFound
}
```

### 9. 입력값 검증 및 변환

```kotlin
fun createUser(username: String?, email: String?): Result<User> {
    return username
        ?.takeIf { it.length >= 3 }
        ?.let { validUsername ->
            email
                ?.takeIf { it.contains("@") }
                ?.let { validEmail ->
                    Result.success(User(
                        username = validUsername,
                        email = validEmail
                    ))
                }
        } ?: Result.failure(IllegalArgumentException("Invalid input"))
}
```

### 10. 파일 처리

```kotlin
fun readConfig(filePath: String): Config? {
    return File(filePath)
        .takeIf { it.exists() }
        ?.let { file ->
            file.readText()
                .let { json ->
                    Json.decodeFromString<Config>(json)
                }
        }
}
```

## let vs apply vs run

```kotlin
val user = User()

// let: 값 변환
val email: String = user.let { it.email }

// apply: 객체 설정 후 객체 반환
val user2: User = user.apply {
    username = "john"
    email = "john@example.com"
}

// run: 객체 설정 + 다른 값 반환
val isValid: Boolean = user.run {
    username = "john"
    email = "john@example.com"
    email.contains("@")
}
```

## Null 안전성 패턴

### 1. 기본 패턴

```kotlin
val length = str?.let { it.length } ?: 0
```

### 2. 중첩된 nullable 처리

```kotlin
// 나쁜 예
val city: String? = null
if (user != null) {
    if (user.address != null) {
        city = user.address.city
    }
}

// 좋은 예
val city = user?.address?.let { it.city }
```

### 3. 여러 nullable 값 조합

```kotlin
fun sendEmail(user: User?, template: Template?) {
    user?.let { u ->
        template?.let { t ->
            emailService.send(u.email, t.content)
        }
    }
}
```

### 4. Null이 아닐 때만 컬렉션에 추가

```kotlin
val users = mutableListOf<User>()

findUser(1)?.let { users.add(it) }
findUser(2)?.let { users.add(it) }
findUser(3)?.let { users.add(it) }
```

## 베스트 프랙티스

### ✅ 권장

1. **Nullable 값 처리**
   ```kotlin
   user?.let { sendEmail(it.email) }
   ```

2. **값 변환**
   ```kotlin
   val dto = user.let { UserDTO(it.username, it.email) }
   ```

3. **체이닝에서 변환**
   ```kotlin
   value.let { it * 2 }.let { it + 10 }
   ```

4. **Elvis 연산자와 조합**
   ```kotlin
   user?.let { it.email } ?: "default@example.com"
   ```

### ❌ 비권장

1. **객체 속성 설정 (apply 사용 권장)**
   ```kotlin
   // Bad
   user.let {
       it.username = "john"
       it.email = "john@example.com"
   }

   // Good
   user.apply {
       username = "john"
       email = "john@example.com"
   }
   ```

2. **단순 메서드 호출 (직접 호출 권장)**
   ```kotlin
   // Bad
   user.let { it.save() }

   // Good
   user.save()
   ```

3. **Non-null 값에 불필요한 let**
   ```kotlin
   // Bad
   val name: String = "John"
   name.let { println(it) }

   // Good
   println(name)
   ```

## 실전 팁

### 1. 명시적 파라미터 이름 사용

```kotlin
// it 대신 명시적 이름 사용
user?.let { currentUser ->
    println("Processing ${currentUser.username}")
    sendEmail(currentUser.email)
}
```

### 2. 중첩 let 피하기

```kotlin
// 나쁜 예: 가독성 떨어짐
value?.let { v ->
    process(v)?.let { result ->
        save(result)?.let { saved ->
            notify(saved)
        }
    }
}

// 좋은 예: Early return 사용
val v = value ?: return
val result = process(v) ?: return
val saved = save(result) ?: return
notify(saved)
```

### 3. takeIf/takeUnless와 조합

```kotlin
val user = findUser(id)
    ?.takeIf { it.isActive }
    ?.let { activeUser ->
        UserDTO(activeUser.username, activeUser.email)
    }
```

## 함수형 프로그래밍 스타일

```kotlin
// 명령형
fun processData(input: String): String {
    val trimmed = input.trim()
    val lowercase = trimmed.lowercase()
    val words = lowercase.split(" ")
    val filtered = words.filter { it.length > 3 }
    val joined = filtered.joinToString("-")
    return joined
}

// 함수형 (let 활용)
fun processData(input: String): String {
    return input
        .let { it.trim() }
        .let { it.lowercase() }
        .let { it.split(" ") }
        .let { it.filter { word -> word.length > 3 } }
        .let { it.joinToString("-") }
}

// 더 간결하게
fun processData(input: String): String {
    return input.trim()
        .lowercase()
        .split(" ")
        .filter { it.length > 3 }
        .joinToString("-")
}
```

## 요약

`let`은 값을 변환하고 nullable 값을 안전하게 처리하는 데 가장 적합한 스코프 함수입니다. 특히 `?.let { }` 패턴은 Kotlin에서 null 안전성을 보장하는 가장 일반적이고 우아한 방법입니다. 함수형 프로그래밍 스타일의 변환 체이닝을 통해 코드를 더 선언적이고 읽기 쉽게 만들 수 있습니다.

## 관련 문서

- [also.md](also.md) - 부수 효과 작업
- [apply.md](apply.md) - 객체 초기화
- [run.md](run.md) - 객체 설정 + 결과 반환
- [with.md](with.md) - 객체 컨텍스트 작업