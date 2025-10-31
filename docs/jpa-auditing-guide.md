# JPA Auditing 적용 가이드

## 개요

JPA Auditing은 엔티티의 생성 시간, 수정 시간 등을 자동으로 관리해주는 기능입니다.
중복 코드를 제거하고 일관성을 보장하며 실수를 방지할 수 있습니다.

## 적용 전후 비교

### 적용 전 (수동 관리) ❌

```kotlin
@Entity
class User(
    val id: Long? = null,
    val email: String,
    var currentPoint: BigDecimal = BigDecimal.ZERO,
    val createdAt: LocalDateTime = LocalDateTime.now(),  // 🔴 수동 설정
    var updatedAt: LocalDateTime = LocalDateTime.now()   // 🔴 수동 설정
) {
    fun addPoint(amount: BigDecimal) {
        this.currentPoint = this.currentPoint.add(amount)
        this.updatedAt = LocalDateTime.now()  // 🔴 매번 수동 업데이트!
    }
}
```

**문제점:**
- 모든 엔티티에 createdAt, updatedAt 중복 선언
- 모든 수정 메서드에서 updatedAt 수동 업데이트 필요
- 깜빡하면 updatedAt 업데이트 누락 가능
- 코드 중복 과다

### 적용 후 (자동 관리) ✅

```kotlin
// BaseEntity.kt - 공통 필드 정의
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity(
    @CreatedDate
    var createdAt: LocalDateTime? = null,

    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,

    val isActive: Boolean = true,
    val isDeleted: Boolean = false
)

// User.kt - BaseEntity 상속
@Entity
class User(
    val id: Long? = null,
    val email: String,
    var currentPoint: BigDecimal = BigDecimal.ZERO
) : BaseEntity() {  // ✅ BaseEntity 상속
    fun addPoint(amount: BigDecimal) {
        this.currentPoint = this.currentPoint.add(amount)
        // updatedAt는 JPA Auditing이 자동으로 업데이트 ✨
    }
}
```

**장점:**
- ✅ 중복 코드 제거 (모든 엔티티에서 반복 선언 불필요)
- ✅ 자동 업데이트 (updatedAt 수동 설정 불필요)
- ✅ 실수 방지 (깜빡임 방지)
- ✅ 일관성 보장 (모든 엔티티에 동일한 방식 적용)

## 구현 단계

### 1단계: BaseEntity 생성

`src/main/kotlin/com/example/kotlin/domain/common/BaseEntity.kt`

```kotlin
package com.example.kotlin.domain.common

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity(
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime? = null,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val isDeleted: Boolean = false
)
```

**어노테이션 설명:**
- `@MappedSuperclass`: 이 클래스를 상속받은 엔티티가 필드를 물려받도록 함
- `@EntityListeners(AuditingEntityListener::class)`: JPA Auditing 리스너 등록
- `@CreatedDate`: 엔티티 생성 시 자동으로 현재 시간 설정
- `@LastModifiedDate`: 엔티티 수정 시 자동으로 현재 시간 업데이트

### 2단계: JPA Auditing 설정

`src/main/kotlin/com/example/kotlin/config/JpaAuditingConfig.kt`

```kotlin
package com.example.kotlin.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@Configuration
@EnableJpaAuditing
class JpaAuditingConfig
```

**어노테이션 설명:**
- `@EnableJpaAuditing`: Spring Data JPA Auditing 기능 활성화

### 3단계: 엔티티 수정

기존 엔티티에서 중복 필드를 제거하고 BaseEntity를 상속받습니다.

#### Before (수정 전)

```kotlin
@Entity
class User(
    val id: Long? = null,
    val email: String,
    var currentPoint: BigDecimal = BigDecimal.ZERO,
    val isActive: Boolean = true,           // 🔴 중복
    val isDeleted: Boolean = false,         // 🔴 중복
    val createdAt: LocalDateTime = LocalDateTime.now(),  // 🔴 중복
    var updatedAt: LocalDateTime = LocalDateTime.now()   // 🔴 중복
) {
    fun addPoint(amount: BigDecimal) {
        this.currentPoint = this.currentPoint.add(amount)
        this.updatedAt = LocalDateTime.now()  // 🔴 수동 업데이트
    }
}
```

#### After (수정 후)

```kotlin
@Entity
class User(
    val id: Long? = null,
    val email: String,
    var currentPoint: BigDecimal = BigDecimal.ZERO
) : BaseEntity() {  // ✅ BaseEntity 상속
    fun addPoint(amount: BigDecimal) {
        this.currentPoint = this.currentPoint.add(amount)
        // updatedAt는 JPA Auditing이 자동으로 업데이트 ✨
    }
}
```

## 동작 원리

### 생성 시점

```kotlin
val user = User(email = "test@ex.com", name = "테스트")
entityManager.persist(user)

// JPA Auditing이 자동으로:
// user.createdAt = LocalDateTime.now()
// user.updatedAt = LocalDateTime.now()
```

### 수정 시점

```kotlin
user.addPoint(BigDecimal(1000))
entityManager.flush()

// JPA Auditing이 자동으로:
// user.updatedAt = LocalDateTime.now()  (createdAt은 변경 안 됨)
```

### 타임라인

```
[생성]
createdAt: 2025-10-31 10:00:00  ← @CreatedDate가 설정
updatedAt: 2025-10-31 10:00:00  ← @LastModifiedDate가 설정

[수정 1]
createdAt: 2025-10-31 10:00:00  (변경 없음, updatable = false)
updatedAt: 2025-10-31 10:05:30  ← @LastModifiedDate가 자동 업데이트

[수정 2]
createdAt: 2025-10-31 10:00:00  (변경 없음)
updatedAt: 2025-10-31 10:12:45  ← @LastModifiedDate가 자동 업데이트
```

## 실전 예제

### 예제 1: 사용자 포인트 관리

```kotlin
@Test
fun `포인트_추가시_updatedAt_자동_업데이트`() {
    // Given
    val user = User(email = "test@ex.com", name = "홍길동")
    entityManager.persist(user)

    val originalUpdatedAt = user.updatedAt
    Thread.sleep(100)

    // When - 포인트 추가 (updatedAt 수동 설정 없음)
    user.addPoint(BigDecimal(5000))
    entityManager.flush()

    // Then - updatedAt가 자동으로 업데이트됨
    assertThat(user.updatedAt).isAfter(originalUpdatedAt)
}
```

### 예제 2: 주문 상태 변경

```kotlin
@Test
fun `주문_확정시_updatedAt_자동_업데이트`() {
    // Given
    val order = Order(
        user = user,
        totalAmount = BigDecimal(50000),
        status = OrderStatus.PENDING
    )
    entityManager.persist(order)

    val originalUpdatedAt = order.updatedAt
    Thread.sleep(100)

    // When - 상태 변경 (updatedAt 수동 설정 없음)
    order.confirm()
    entityManager.flush()

    // Then
    assertThat(order.status).isEqualTo(OrderStatus.CONFIRMED)
    assertThat(order.updatedAt).isAfter(originalUpdatedAt)
}
```

### 예제 3: 상품 재고 관리

```kotlin
@Test
fun `재고_변경시_updatedAt_자동_업데이트`() {
    // Given
    val item = Item(
        name = "노트북",
        basePrice = BigDecimal(1000000),
        stockQuantity = 100
    )
    entityManager.persist(item)

    val originalUpdatedAt = item.updatedAt
    Thread.sleep(100)

    // When - 재고 감소 (updatedAt 수동 설정 없음)
    item.decreaseStock(10)
    entityManager.flush()

    // Then
    assertThat(item.stockQuantity).isEqualTo(90)
    assertThat(item.updatedAt).isAfter(originalUpdatedAt)
}
```

## BaseEntity 필드 설명

### createdAt (생성 시간)

- **타입**: `LocalDateTime?`
- **설정**: `@CreatedDate`
- **동작**: 엔티티 최초 저장 시 자동 설정
- **변경**: 불가 (`updatable = false`)
- **용도**: 언제 생성되었는지 추적

### updatedAt (수정 시간)

- **타입**: `LocalDateTime?`
- **설정**: `@LastModifiedDate`
- **동작**: 엔티티 수정 시 자동 업데이트
- **변경**: 자동 (JPA가 관리)
- **용도**: 마지막 수정 시점 추적

### isActive (활성화 상태)

- **타입**: `Boolean`
- **기본값**: `true`
- **용도**: 엔티티 활성화 여부 (비활성화 시 조회에서 제외 가능)

### isDeleted (소프트 삭제 플래그)

- **타입**: `Boolean`
- **기본값**: `false`
- **용도**: 실제 삭제 대신 플래그로 삭제 표시 (데이터 복구 가능)

## 소프트 삭제 패턴

```kotlin
// 하드 삭제 (실제 DB에서 삭제) - 복구 불가 ❌
fun hardDelete(user: User) {
    entityManager.remove(user)
}

// 소프트 삭제 (플래그만 변경) - 복구 가능 ✅
fun softDelete(user: User) {
    user.isDeleted = true
    // updatedAt는 자동으로 업데이트됨
}

// 활성 사용자만 조회
fun findActiveUsers(): List<User> {
    return entityManager
        .createQuery("SELECT u FROM User u WHERE u.isDeleted = false", User::class.java)
        .resultList
}
```

## 고급 활용

### 1. 생성자/수정자 추적

BaseEntity를 확장하여 누가 생성/수정했는지도 추적할 수 있습니다.

```kotlin
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class AuditableEntity(
    @CreatedDate
    var createdAt: LocalDateTime? = null,

    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,

    @CreatedBy
    var createdBy: String? = null,

    @LastModifiedBy
    var modifiedBy: String? = null
)

// AuditorAware 구현 필요
@Component
class AuditorAwareImpl : AuditorAware<String> {
    override fun getCurrentAuditor(): Optional<String> {
        // Spring Security 등에서 현재 사용자 정보 가져오기
        return Optional.of("system")
    }
}
```

### 2. 엔티티별 커스터마이징

특정 엔티티만 다르게 동작시킬 수 있습니다.

```kotlin
// 읽기 전용 엔티티 (updatedAt 불필요)
@Entity
class LogEntry(
    val message: String,
    val level: String
) : BaseEntity() {
    // updatedAt는 상속받지만 실제로 업데이트되지 않음
    // (읽기 전용이므로 변경 메서드가 없음)
}
```

## 문제 해결

### Q1. createdAt/updatedAt이 null로 설정됨

**원인**: `@EnableJpaAuditing`이 설정되지 않음

**해결**:
```kotlin
@Configuration
@EnableJpaAuditing  // 이 어노테이션 필수!
class JpaAuditingConfig
```

### Q2. updatedAt이 업데이트되지 않음

**원인 1**: 트랜잭션 내에서 flush() 호출 안 함

**해결**:
```kotlin
@Transactional
fun updateUser(user: User) {
    user.addPoint(BigDecimal(1000))
    entityManager.flush()  // flush 호출 필요
}
```

**원인 2**: Dirty Checking이 동작하지 않음

**해결**: var 필드를 변경해야 Dirty Checking 동작
```kotlin
// ✅ OK: var 필드 변경
user.currentPoint = newValue

// ❌ NO: val 필드는 변경 불가
user.email = newValue  // 컴파일 에러
```

### Q3. 테스트에서 Auditing이 동작하지 않음

**원인**: 테스트에서 JpaAuditingConfig를 import하지 않음

**해결**:
```kotlin
@DataJpaTest
@Import(JpaAuditingConfig::class)  // 이 줄 추가!
class MyTest {
    // ...
}
```

## 테스트 실행

```bash
# JPA Auditing 테스트 실행
./gradlew test --tests "com.example.kotlin.practice.JpaAuditingTest"
```

## 요약

| 항목 | 적용 전 | 적용 후 |
|------|---------|---------|
| **코드 중복** | 모든 엔티티에 반복 | BaseEntity 한 곳 |
| **수동 업데이트** | 모든 메서드에 필요 | 자동 처리 |
| **실수 가능성** | 높음 | 낮음 |
| **유지보수성** | 어려움 | 쉬움 |
| **일관성** | 보장 안 됨 | 보장됨 |

## 적용된 파일 목록

```
src/main/kotlin/com/example/kotlin/
├── config/
│   └── JpaAuditingConfig.kt  ← 설정 파일
├── domain/
│   ├── common/
│   │   └── BaseEntity.kt     ← Base 엔티티
│   ├── user/
│   │   └── User.kt           ← BaseEntity 상속
│   ├── point/
│   │   └── Point.kt          ← BaseEntity 상속
│   ├── item/
│   │   ├── Item.kt           ← BaseEntity 상속
│   │   └── ItemOption.kt     ← BaseEntity 상속
│   └── order/
│       ├── Order.kt          ← BaseEntity 상속
│       └── OrderItem.kt      ← BaseEntity 상속
```

## 참고 자료

- [Spring Data JPA - Auditing](https://docs.spring.io/spring-data/jpa/reference/auditing.html)
- [JPA @MappedSuperclass](https://docs.oracle.com/javaee/7/api/javax/persistence/MappedSuperclass.html)
