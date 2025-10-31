# Kotlin JPA 엔티티 설계 패턴

## 질문: 왜 모든 컬럼을 생성자에 넣었나?

### 현재 패턴 (주 생성자 방식)

```kotlin
@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    var currentPoint: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

## 3가지 패턴 비교

### 패턴 1: 주 생성자 방식 (Primary Constructor) ✅ 현재 방식

```kotlin
@Entity
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val name: String,
    var age: Int = 0
) {
    fun updateAge(newAge: Int) {
        this.age = newAge
    }
}
```

**장점:**
- ✅ **Kotlin 관용적 스타일**: Kotlin에서 권장하는 방식
- ✅ **간결성**: 필드 선언과 생성자를 한 번에 처리
- ✅ **불변성**: `val` 필드를 명확하게 표현
- ✅ **기본값 지원**: 파라미터 기본값으로 다양한 생성자 조합 가능
- ✅ **테스트 용이성**: 객체 생성이 간단하고 명확

**단점:**
- ⚠️ JPA 어노테이션이 생성자에 섞여 가독성 저하 가능
- ⚠️ 생성자가 길어지면 복잡해 보임

### 패턴 2: 본문 프로퍼티 방식 (Body Properties)

```kotlin
@Entity
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    lateinit var name: String

    var age: Int = 0

    // JPA용 기본 생성자 (protected)
    protected constructor()

    // 실제 사용할 생성자
    constructor(name: String, age: Int) : this() {
        this.name = name
        this.age = age
    }
}
```

**장점:**
- ✅ JPA 어노테이션이 명확하게 분리됨
- ✅ Java JPA 패턴과 유사 (익숙함)

**단점:**
- ❌ **lateinit 필수**: 불변성 보장 불가 (모두 var)
- ❌ **장황함**: Kotlin의 간결함 상실
- ❌ **생성자 중복**: 기본 생성자 + 실제 생성자
- ❌ **Null 안전성 약화**: lateinit 사용으로 NPE 위험

### 패턴 3: 하이브리드 방식 (Hybrid)

```kotlin
@Entity
class User(
    name: String,
    age: Int
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    var name: String = name
        protected set  // 외부에서 수정 불가

    @Column(nullable = false)
    var age: Int = age
        protected set
}
```

**장점:**
- ✅ 생성자는 간단하게, 어노테이션은 본문에
- ✅ protected set으로 불변성 부분 보장

**단점:**
- ⚠️ 여전히 var만 사용 가능
- ⚠️ 코드 중복 (name: String 두 번 작성)

## 왜 패턴 1(주 생성자)을 선택했는가?

### 이유 1: Kotlin 철학과 일치

Kotlin은 간결성과 명확성을 중시합니다.

```kotlin
// Kotlin 스타일 ✅
data class Person(val name: String, val age: Int)

// Java 스타일 ❌
class Person {
    private final String name;
    private final int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

### 이유 2: 불변성 보장

```kotlin
// 패턴 1: val로 불변 필드 명확히 표현 ✅
class User(
    val email: String,  // 변경 불가
    var name: String    // 변경 가능
)

// 패턴 2: 모두 var, 불변성 없음 ❌
class User {
    lateinit var email: String  // 실제로는 변경하면 안 되지만 가능
    lateinit var name: String
}
```

### 이유 3: 테스트 편의성

```kotlin
// 패턴 1: 간단하고 명확 ✅
val user = User(
    email = "test@example.com",
    name = "테스트"
)

// 패턴 2: 번거로움 ❌
val user = User()
user.email = "test@example.com"
user.name = "테스트"
```

### 이유 4: 기본값으로 다양한 생성 패턴

```kotlin
class User(
    val id: Long? = null,
    val email: String,
    val name: String,
    val age: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

// 다양한 생성 방식 가능
val user1 = User(email = "a@a.com", name = "Kim")
val user2 = User(email = "b@b.com", name = "Lee", age = 30)
val user3 = User(id = 1L, email = "c@c.com", name = "Park")
```

## JPA 호환성 해결

### 문제: JPA는 기본 생성자 필요

JPA/Hibernate는 리플렉션으로 객체를 생성하기 위해 기본 생성자(no-arg constructor)가 필요합니다.

### 해결: kotlin-jpa 플러그인

```gradle
plugins {
    id 'org.jetbrains.kotlin.plugin.jpa' version '1.9.25'
}
```

이 플러그인이 자동으로:
1. **기본 생성자 생성**: 모든 JPA 엔티티에 no-arg constructor 추가
2. **open 키워드 추가**: JPA 프록시를 위해 클래스를 open으로 변경

```kotlin
// 개발자가 작성한 코드
@Entity
class User(val name: String)

// 플러그인이 컴파일 시 생성하는 것 (개념적)
@Entity
open class User(val name: String) {
    protected constructor() : this("")  // 기본 생성자 자동 생성
}
```

## 실전 권장 패턴

### 권장: 주 생성자 + 명확한 구분

```kotlin
@Entity
@Table(name = "users")
class User(
    // === 식별자 ===
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // === 비즈니스 필수 필드 (불변) ===
    @Column(nullable = false, unique = true, length = 100)
    val email: String,

    @Column(nullable = false, length = 50)
    val name: String,

    // === 가변 필드 ===
    @Column(nullable = false)
    var currentPoint: BigDecimal = BigDecimal.ZERO,

    // === 메타 정보 ===
    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val isDeleted: Boolean = false,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // 비즈니스 로직 메서드
    fun addPoint(amount: BigDecimal) {
        this.currentPoint = this.currentPoint.add(amount)
        this.updatedAt = LocalDateTime.now()
    }
}
```

**구조:**
1. **생성자**: 필드 선언 + JPA 매핑
2. **본문**: 비즈니스 로직만
3. **주석**: 필드를 논리적 그룹으로 구분

### 언제 본문 프로퍼티를 사용할까?

**케이스 1: 양방향 연관관계**

```kotlin
@Entity
class Item(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val name: String,
    val price: BigDecimal
) {
    // 양방향 관계는 본문에 선언 (순환 참조 방지)
    @OneToMany(mappedBy = "item", cascade = [CascadeType.ALL])
    val options: MutableList<ItemOption> = mutableListOf()

    fun addOption(option: ItemOption) {
        options.add(option)
    }
}
```

**케이스 2: 계산 필드 (Transient)**

```kotlin
@Entity
class Order(
    @Id @GeneratedValue
    val id: Long? = null,

    val totalAmount: BigDecimal,
    val discountAmount: BigDecimal
) {
    // 계산 필드는 본문에
    val finalAmount: BigDecimal
        get() = totalAmount.subtract(discountAmount)
}
```

## 성능 고려사항

### 지연 로딩 주의

```kotlin
// 연관관계는 항상 본문에 선언
@Entity
class User(
    val id: Long? = null,
    val name: String
) {
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    val orders: MutableList<Order> = mutableListOf()
}

// ❌ 이렇게 하지 마세요 (생성자에 연관관계)
@Entity
class User(
    val id: Long? = null,
    val name: String,
    @OneToMany(mappedBy = "user")
    val orders: MutableList<Order> = mutableListOf()  // 초기화 시점 문제
)
```

## 마이그레이션 가이드

### Java JPA → Kotlin JPA

**Before (Java):**
```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    protected User() {}  // JPA용

    public User(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

**After (Kotlin):**
```kotlin
@Entity
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var name: String
)
```

**라인 수 비교:**
- Java: 19 라인
- Kotlin: 6 라인
- **68% 감소!**

## 요약

### ✅ 주 생성자 방식을 사용하는 이유

| 이유 | 설명 |
|------|------|
| **Kotlin 철학** | 간결하고 명확한 코드 |
| **불변성** | val/var로 의도 명확히 표현 |
| **테스트성** | 객체 생성이 간단하고 직관적 |
| **기본값** | 다양한 생성자 조합 지원 |
| **JPA 호환** | kotlin-jpa 플러그인이 자동 처리 |

### 📋 필드 배치 가이드

| 필드 종류 | 위치 | 이유 |
|-----------|------|------|
| 식별자 (id) | 생성자 | 필수 필드 |
| 기본 필드 | 생성자 | 간결성 |
| 양방향 연관관계 | 본문 | 순환 참조 방지 |
| 계산 필드 | 본문 | 로직 분리 |
| 비즈니스 메서드 | 본문 | 명확한 구조 |

### 🎯 최종 권장 패턴

```kotlin
@Entity
class YourEntity(
    // 1. 식별자
    @Id @GeneratedValue
    val id: Long? = null,

    // 2. 불변 필드 (val)
    val immutableField: String,

    // 3. 가변 필드 (var)
    var mutableField: Int = 0,

    // 4. 메타 정보
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    // 5. 양방향 관계
    @OneToMany(mappedBy = "parent")
    val children: MutableList<Child> = mutableListOf()

    // 6. 비즈니스 로직
    fun businessMethod() { }
}
```

이 패턴이 Kotlin + JPA의 **Best Practice**입니다! 🎉
