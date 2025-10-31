# Kotlin Collections: associateBy vs groupBy

## 핵심 차이점

| 구분 | associateBy | groupBy |
|------|-------------|---------|
| **반환 타입** | `Map<K, V>` | `Map<K, List<V>>` |
| **매핑** | 1:1 (키 → 값) | 1:N (키 → 값 리스트) |
| **중복 키** | 마지막 값으로 덮어쓰기 | 모든 값을 리스트에 보존 |
| **사용 목적** | 고유 키로 빠른 조회 | 같은 키로 그룹화 |
| **성능** | O(1) 조회 | O(1) 조회, 그룹당 O(n) 처리 |

## 시각적 비교

### associateBy - 1:1 매핑

```
원본 데이터:
User(id=1, name="김철수", city="서울")
User(id=2, name="이영희", city="부산")
User(id=3, name="박민수", city="서울")

users.associateBy { it.city }
               ↓
Map<String, User> {
  "서울" -> User(id=3, name="박민수", city="서울")  ⚠️ 김철수는 덮어써짐!
  "부산" -> User(id=2, name="이영희", city="부산")
}
```

### groupBy - 1:N 매핑

```
원본 데이터:
User(id=1, name="김철수", city="서울")
User(id=2, name="이영희", city="부산")
User(id=3, name="박민수", city="서울")

users.groupBy { it.city }
               ↓
Map<String, List<User>> {
  "서울" -> [
    User(id=1, name="김철수", city="서울"),
    User(id=3, name="박민수", city="서울")  ✅ 모두 보존됨!
  ],
  "부산" -> [
    User(id=2, name="이영희", city="부산")
  ]
}
```

## 1. associateBy 상세 설명

### 기본 사용법

```kotlin
val users = listOf(
    User(1L, "김철수", 25, "서울"),
    User(2L, "이영희", 30, "부산"),
    User(3L, "박민수", 25, "서울")
)

// ID를 키로 하는 Map 생성
val userMap: Map<Long, User> = users.associateBy { it.id }
// 결과: {1=User(...), 2=User(...), 3=User(...)}

// 특정 사용자 빠르게 조회 (O(1))
val user = userMap[2L]  // User(2L, "이영희", ...)
```

### 키와 값을 동시에 변환

```kotlin
// ID를 키로, 이름을 값으로
val nameMap: Map<Long, String> = users.associateBy(
    keySelector = { it.id },
    valueTransform = { it.name }
)
// 결과: {1="김철수", 2="이영희", 3="박민수"}
```

### 중복 키 처리 주의사항

```kotlin
// ⚠️ city를 키로 사용 (중복 발생)
val cityMap = users.associateBy { it.city }
// 결과: {"서울"=User(3, "박민수"), "부산"=User(2, "이영희")}
// 주의: "서울"의 김철수는 박민수로 덮어써짐!
```

### 실전 활용 패턴

```kotlin
// 패턴 1: 엔티티 ID로 빠른 조회용 캐시
val orderCache: Map<Long, Order> = orders.associateBy { it.id }
val order = orderCache[orderId]  // O(1) 조회

// 패턴 2: 외래키 조인
val userMap = users.associateBy { it.id }
orders.forEach { order ->
    val user = userMap[order.userId]  // 빠른 조인
    println("${user?.name}의 주문: ${order.amount}원")
}

// 패턴 3: Enum이나 코드값을 키로
enum class Status { PENDING, COMPLETED, CANCELLED }
val statusMessages = Status.values().associateBy(
    keySelector = { it },
    valueTransform = {
        when(it) {
            Status.PENDING -> "처리 중"
            Status.COMPLETED -> "완료"
            Status.CANCELLED -> "취소됨"
        }
    }
)
```

## 2. groupBy 상세 설명

### 기본 사용법

```kotlin
val users = listOf(
    User(1L, "김철수", 25, "서울"),
    User(2L, "이영희", 30, "부산"),
    User(3L, "박민수", 25, "서울")
)

// 나이별로 그룹화
val ageGroups: Map<Int, List<User>> = users.groupBy { it.age }
// 결과: {
//   25 = [User(1, "김철수", 25), User(3, "박민수", 25)],
//   30 = [User(2, "이영희", 30)]
// }

// 특정 나이 그룹 조회
val age25Users = ageGroups[25]  // [김철수, 박민수]
```

### 값을 변환하면서 그룹화

```kotlin
// 나이를 키로, 이름만 추출
val ageNames: Map<Int, List<String>> = users.groupBy(
    keySelector = { it.age },
    valueTransform = { it.name }
)
// 결과: {25=["김철수", "박민수"], 30=["이영희"]}
```

### 실전 활용 패턴

```kotlin
// 패턴 1: 카테고리별 통계
val ordersByStatus: Map<String, List<Order>> = orders.groupBy { it.status }
ordersByStatus.forEach { (status, orderList) ->
    val count = orderList.size
    val total = orderList.sumOf { it.amount }
    println("[$status] ${count}건, 총액: ${total}원")
}

// 패턴 2: 사용자별 집계
val ordersByUser: Map<Long, List<Order>> = orders.groupBy { it.userId }
ordersByUser.forEach { (userId, userOrders) ->
    val totalSpent = userOrders.sumOf { it.amount }
    println("사용자 $userId: 총 구매액 ${totalSpent}원")
}

// 패턴 3: 날짜별 그룹화
val ordersByDate = orders.groupBy { it.createdAt.toLocalDate() }
ordersByDate.forEach { (date, dayOrders) ->
    println("$date: ${dayOrders.size}건 주문")
}

// 패턴 4: 복잡한 조건으로 그룹화
val orderGroups = orders.groupBy { order ->
    when {
        order.amount >= 50000 -> "고액"
        order.amount >= 20000 -> "중액"
        else -> "소액"
    }
}
```

## 3. 실전 조합 패턴

### 패턴 A: 조인 후 그룹화

```kotlin
// 1단계: userId로 User 맵 생성 (빠른 조회용)
val userMap = users.associateBy { it.id }

// 2단계: 주문을 상태별로 그룹화
val ordersByStatus = orders.groupBy { it.status }

// 3단계: 각 그룹의 사용자 정보와 결합
ordersByStatus.forEach { (status, orderList) ->
    println("[$status] 주문 목록:")
    orderList.forEach { order ->
        val user = userMap[order.userId]
        println("  - ${user?.name}: ${order.amount}원")
    }
}
```

### 패턴 B: 다중 레벨 그룹화

```kotlin
// 도시별 -> 나이별 그룹화
val cityAgeGroups: Map<String, Map<Int, List<User>>> = users
    .groupBy { it.city }
    .mapValues { (_, cityUsers) ->
        cityUsers.groupBy { it.age }
    }

// 결과: {
//   "서울" = {
//     25 = [User(1, "김철수"), User(3, "박민수")],
//     30 = [User(5, "최상욱")]
//   },
//   "부산" = {
//     30 = [User(2, "이영희")]
//   }
// }

// 사용 예시
cityAgeGroups["서울"]?.get(25)?.forEach { user ->
    println("서울의 25세: ${user.name}")
}
```

### 패턴 C: 필터링 + 그룹화

```kotlin
// 완료된 주문만 사용자별로 그룹화
val completedOrdersByUser = orders
    .filter { it.status == "COMPLETED" }
    .groupBy { it.userId }

// 고액 주문자만 추출
val bigSpenders = completedOrdersByUser
    .filter { (_, orderList) ->
        orderList.sumOf { it.amount } >= 100000
    }
    .mapKeys { (userId, _) -> userMap[userId]?.name ?: "Unknown" }
```

## 4. 성능 고려사항

### associateBy 성능

```kotlin
// O(n) 생성, O(1) 조회
val userMap = users.associateBy { it.id }  // O(n)
val user = userMap[2L]  // O(1)

// vs List.find()
val user = users.find { it.id == 2L }  // O(n) - 매번 탐색
```

**권장 사항:**
- 같은 리스트를 여러 번 조회할 경우 associateBy 사용
- 한 번만 조회할 경우 find() 사용
- 임계점: 약 3번 이상 조회 시 Map이 유리

### groupBy 성능

```kotlin
// O(n) 생성 + O(m) 그룹당 처리
val groups = orders.groupBy { it.userId }  // O(n)

// 그룹별 처리
groups.forEach { (_, orderList) ->  // O(m) per group
    orderList.sumOf { it.amount }
}
```

**권장 사항:**
- 그룹별 집계가 필요한 경우 groupBy 사용
- 전체 데이터 집계만 필요하면 직접 계산이 더 효율적

```kotlin
// groupBy 사용 (그룹별 통계 필요)
val statusGroups = orders.groupBy { it.status }
statusGroups.forEach { (status, list) ->
    println("$status: ${list.size}건")
}

// 직접 계산 (전체 통계만 필요)
val totalCompleted = orders.count { it.status == "COMPLETED" }
```

## 5. 언제 무엇을 사용할까?

### ✅ associateBy를 사용하는 경우

1. **ID로 엔티티 조회**
   ```kotlin
   val userMap = users.associateBy { it.id }
   val user = userMap[userId]
   ```

2. **외래키 조인**
   ```kotlin
   val userMap = users.associateBy { it.id }
   orders.forEach { order ->
       val user = userMap[order.userId]
   }
   ```

3. **고유 키-값 매핑**
   ```kotlin
   val codeMap = codes.associateBy { it.code }
   val message = codeMap["ERROR_001"]?.message
   ```

### ✅ groupBy를 사용하는 경우

1. **카테고리별 통계**
   ```kotlin
   val statusGroups = orders.groupBy { it.status }
   statusGroups.forEach { (status, list) ->
       println("$status: ${list.sumOf { it.amount }}원")
   }
   ```

2. **사용자별 집계**
   ```kotlin
   val userOrders = orders.groupBy { it.userId }
   userOrders.forEach { (userId, list) ->
       println("사용자 $userId: ${list.size}건")
   }
   ```

3. **시간대별 분석**
   ```kotlin
   val hourlyOrders = orders.groupBy {
       it.createdAt.hour
   }
   ```

## 6. 흔한 실수와 해결책

### 실수 1: groupBy가 필요한데 associateBy 사용

```kotlin
❌ 잘못된 코드 (중복 키로 데이터 손실)
val cityUsers = users.associateBy { it.city }
// "서울"의 여러 사용자 중 마지막만 남음!

✅ 올바른 코드
val cityUsers = users.groupBy { it.city }
// "서울"의 모든 사용자 보존
```

### 실수 2: 불필요한 groupBy 사용

```kotlin
❌ 비효율적 (전체 집계만 필요한 경우)
val groups = orders.groupBy { it.status }
val completedCount = groups["COMPLETED"]?.size ?: 0

✅ 효율적
val completedCount = orders.count { it.status == "COMPLETED" }
```

### 실수 3: null 처리 누락

```kotlin
❌ NPE 위험
val user = userMap[userId]
println(user.name)  // NPE 가능

✅ 안전한 처리
val user = userMap[userId]
println(user?.name ?: "Unknown")

// 또는
userMap[userId]?.let { user ->
    println(user.name)
}
```

## 7. 테스트 코드 실행 방법

```bash
# 전체 테스트 실행
./gradlew test --tests "com.example.kotlin.practice.AssociateByGroupByTest"

# 특정 테스트만 실행
./gradlew test --tests "*.AssociateByGroupByTest.associateBy는 고유한 키로 1대1 매핑한다"

# 출력 확인하며 실행
./gradlew test --tests "*.AssociateByGroupByTest" --info
```

## 8. 추가 학습 자료

### 관련 함수들

```kotlin
// associate: 키와 값을 Pair로 생성
val map = users.associate { it.id to it.name }
// {1="김철수", 2="이영희", ...}

// associateWith: 키는 원소, 값은 계산
val ageMap = users.associateWith { it.age }
// {User(1)=25, User(2)=30, ...}

// partition: Boolean으로 2개 그룹으로 분리
val (adults, minors) = users.partition { it.age >= 20 }

// groupingBy: 지연 그룹화 (lazy)
val counts = users.groupingBy { it.city }.eachCount()
// {"서울"=3, "부산"=1, ...}
```

### Kotlin 공식 문서
- [Collections Overview](https://kotlinlang.org/docs/collections-overview.html)
- [Grouping](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/grouping-by.html)

## 요약

| 상황 | 사용 함수 | 이유 |
|------|----------|------|
| ID로 빠른 조회 | `associateBy` | O(1) 조회 성능 |
| 외래키 조인 | `associateBy` | 효율적인 조인 |
| 카테고리별 통계 | `groupBy` | 그룹별 집계 필요 |
| 사용자별 목록 | `groupBy` | 1:N 관계 유지 |
| 중복 키 있음 | `groupBy` | 모든 값 보존 |
| 고유 키 보장 | `associateBy` | 간결한 Map 생성 |
