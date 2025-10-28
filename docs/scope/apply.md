# apply - 객체 초기화 및 설정에 특화된 함수

## 개요

`apply`는 객체의 속성을 설정하고 초기화하는 데 특화된 스코프 함수입니다. 객체를 생성하거나 설정한 후 그 객체 자체를 반환할 때 주로 사용합니다.

## 핵심 특징

- **컨텍스트 객체**: `this` (생략 가능)
- **반환값**: 컨텍스트 객체 자체
- **주 용도**: 객체의 속성 초기화 및 설정
- **의미**: "이 객체를 적용(apply)하여 설정하라"

## 기본 문법

```kotlin
val result = someObject.apply {
    // this를 사용 (생략 가능)
    property1 = value1
    property2 = value2
    method()
}
// result는 someObject와 동일
```

## 언제 사용하는가?

1. **객체 생성 직후 여러 속성을 한 번에 초기화할 때**
   - 빌더 패턴 대체

2. **기존 객체의 여러 속성을 변경할 때**
   - 여러 setter 호출을 간결하게

3. **객체를 설정하고 그 객체를 반환해야 할 때**
   - 메서드 체이닝

4. **안드로이드 View 설정, Builder 패턴 구현**
   - UI 컴포넌트 초기화

## this vs it 비교

```kotlin
// apply: this 사용 (생략 가능)
val user = User().apply {
    id = 1                    // this.id
    username = "john"         // this.username
    email = "john@example.com" // this.email
}

// also: it 사용
val user = User().also { user ->
    user.id = 1
    user.username = "john"
    user.email = "john@example.com"
}
```

**apply**를 사용하면 객체의 멤버에 직접 접근하는 것처럼 느껴져 더 자연스럽습니다.

## 실무 예제

### 1. 객체 생성 및 초기화

```kotlin
data class User(
    var id: Long = 0,
    var username: String = "",
    var email: String = "",
    var role: String = "USER"
)

// apply 없이
val user1 = User()
user1.id = 1
user1.username = "john"
user1.email = "john@example.com"
user1.role = "ADMIN"

// apply 사용 - 훨씬 간결!
val user2 = User().apply {
    id = 1
    username = "john"
    email = "john@example.com"
    role = "ADMIN"
}
```

### 2. 빌더 패턴 대체

```kotlin
// 전통적인 빌더 패턴
class UserBuilder {
    private var id: Long = 0
    private var username: String = ""
    private var email: String = ""

    fun setId(id: Long) = apply { this.id = id }
    fun setUsername(username: String) = apply { this.username = username }
    fun setEmail(email: String) = apply { this.email = email }
    fun build() = User(id, username, email)
}

// apply를 사용한 간단한 방법
val user = User().apply {
    id = 1
    username = "john"
    email = "john@example.com"
}
```

### 3. 여러 객체 속성 변경

```kotlin
fun updateUserProfile(user: User, newEmail: String, newRole: String): User {
    return user.apply {
        email = newEmail
        role = newRole
        lastModifiedAt = LocalDateTime.now()
    }
}
```

### 4. 컬렉션 초기화

```kotlin
val config = mutableMapOf<String, Any>().apply {
    put("host", "localhost")
    put("port", 8080)
    put("timeout", 5000)
    put("retries", 3)
    put("ssl", mapOf(
        "enabled" to true,
        "protocol" to "TLS1.3"
    ))
}
```

### 5. 안드로이드 View 설정

```kotlin
val textView = TextView(context).apply {
    text = "Hello World"
    textSize = 18f
    setTextColor(Color.BLACK)
    setPadding(16, 16, 16, 16)
    gravity = Gravity.CENTER
    background = getDrawable(R.drawable.bg_rounded)
}
```

### 6. 데이터베이스 엔티티 생성

```kotlin
fun createOrder(userId: Long, items: List<OrderItem>): Order {
    return Order().apply {
        this.userId = userId
        this.items = items
        status = "PENDING"
        totalAmount = items.sumOf { it.price * it.quantity }
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }
}
```

### 7. 설정 객체 구성

```kotlin
val serverConfig = ServerConfig().apply {
    host = "0.0.0.0"
    port = 8080
    maxConnections = 1000

    ssl = SSLConfig().apply {
        enabled = true
        keyStorePath = "/path/to/keystore"
        protocol = "TLS1.3"
    }

    database = DatabaseConfig().apply {
        url = "jdbc:postgresql://localhost:5432/mydb"
        username = "admin"
        password = "secret"
        maxPoolSize = 20
    }
}
```

### 8. DTO → Entity 변환

```kotlin
fun UserDTO.toEntity(): User {
    return User().apply {
        id = this@toEntity.id
        username = this@toEntity.username
        email = this@toEntity.email
        firstName = this@toEntity.firstName
        lastName = this@toEntity.lastName
        role = this@toEntity.role
        createdAt = LocalDateTime.now()
    }
}
```

### 9. 테스트 픽스처 생성

```kotlin
fun createTestUser(
    username: String = "testuser",
    email: String = "test@example.com"
): User {
    return User().apply {
        id = Random.nextLong()
        this.username = username
        this.email = email
        role = "USER"
        isActive = true
        createdAt = LocalDateTime.now()
    }
}
```

### 10. 복잡한 객체 그래프 생성

```kotlin
val company = Company().apply {
    name = "TechCorp"
    website = "https://techcorp.com"

    address = Address().apply {
        street = "123 Tech Street"
        city = "San Francisco"
        state = "CA"
        zipCode = "94102"
    }

    ceo = Employee().apply {
        firstName = "John"
        lastName = "Doe"
        email = "john.doe@techcorp.com"
        position = "CEO"
    }

    departments = listOf(
        Department().apply {
            name = "Engineering"
            headCount = 50
            budget = BigDecimal("5000000")
        },
        Department().apply {
            name = "Sales"
            headCount = 30
            budget = BigDecimal("3000000")
        }
    )
}
```

## apply + also 패턴

객체 설정과 부수 효과를 분리하는 일반적인 패턴:

```kotlin
fun createUser(username: String, email: String): User {
    return User()
        .apply {
            // 객체 설정
            this.username = username
            this.email = email
            role = "USER"
            createdAt = LocalDateTime.now()
        }
        .also {
            // 부수 효과
            logger.info("User created: ${it.username}")
            eventBus.publish(UserCreatedEvent(it))
            cache.put("user:${it.id}", it)
        }
}
```

## apply vs let vs run

```kotlin
// apply: 객체 설정 후 객체 반환
val user = User().apply {
    username = "john"
    email = "john@example.com"
}
// user는 User 타입

// let: 객체를 변환하여 다른 값 반환
val username = User().let {
    it.username = "john"
    it.username  // String 반환
}
// username은 String 타입

// run: 객체 설정 + 다른 값 반환
val isValid = User().run {
    username = "john"
    email = "john@example.com"
    username.isNotEmpty() && email.contains("@")
}
// isValid는 Boolean 타입
```

## 베스트 프랙티스

### ✅ 권장

1. **객체 초기화**
   ```kotlin
   val user = User().apply {
       id = 1
       username = "john"
   }
   ```

2. **빌더 패턴 대체**
   ```kotlin
   val config = Config().apply {
       host = "localhost"
       port = 8080
   }
   ```

3. **중첩 객체 초기화**
   ```kotlin
   val order = Order().apply {
       items = listOf(
           OrderItem().apply {
               productId = 1
               quantity = 2
           }
       )
   }
   ```

### ❌ 비권장

1. **부수 효과 작업 (also 사용 권장)**
   ```kotlin
   // Bad
   user.apply {
       logger.info("User: $username")
   }

   // Good
   user.also {
       logger.info("User: ${it.username}")
   }
   ```

2. **값 변환 (let/map 사용 권장)**
   ```kotlin
   // Bad
   val name = user.apply {
       username.uppercase()
   }

   // Good
   val name = user.let { it.username.uppercase() }
   ```

3. **null 체크 (let 사용 권장)**
   ```kotlin
   // Bad
   nullableUser?.apply {
       process(username)
   }

   // Good
   nullableUser?.let {
       process(it.username)
   }
   ```

## 실전 팁

### 1. this@ 레이블 사용

중첩된 apply에서 외부 객체 참조:

```kotlin
class Outer {
    val value = "outer"

    fun create() = Inner().apply {
        this.value = "inner"           // Inner의 value
        this@Outer.value               // Outer의 value
    }
}
```

### 2. 함수 참조와 함께 사용

```kotlin
fun User.setupDefaults() = apply {
    role = "USER"
    isActive = true
    createdAt = LocalDateTime.now()
}

val user = User()
    .apply { username = "john" }
    .setupDefaults()
```

### 3. 조건부 설정

```kotlin
val user = User().apply {
    username = "john"
    email = "john@example.com"

    if (isPremium) {
        role = "PREMIUM_USER"
        subscriptionEndDate = LocalDate.now().plusYears(1)
    }
}
```

## 요약

`apply`는 객체의 여러 속성을 초기화하고 설정할 때 가장 적합한 스코프 함수입니다. `this`를 통해 객체의 멤버에 직접 접근하는 것처럼 느껴져 코드가 매우 자연스럽고 읽기 쉽습니다. 객체 생성과 동시에 초기화가 필요한 경우, 전통적인 빌더 패턴을 대체하여 더 간결하고 Kotlin다운 코드를 작성할 수 있습니다.

## 관련 문서

- [also.md](also.md) - 부수 효과 작업
- [let.md](let.md) - 값 변환
- [run.md](run.md) - 객체 설정 + 결과 반환
- [with.md](with.md) - 객체 컨텍스트 작업
