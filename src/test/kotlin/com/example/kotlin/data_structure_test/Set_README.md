# Kotlin Set 자료구조

## 개요

Set은 **중복을 허용하지 않는 컬렉션**입니다. 수학의 집합(Set) 개념을 프로그래밍에 구현한 것으로, 동일한 요소는 하나만 저장됩니다.

## Set의 특징

| 특징 | 설명 |
|------|------|
| 중복 불허 | 동일한 요소는 한 번만 저장됨 |
| equals/hashCode | 요소의 동등성은 `equals()`로, 해시값은 `hashCode()`로 판단 |
| null 허용 | 최대 하나의 null 값 저장 가능 |
| 순서 | 구현체에 따라 다름 (아래 참조) |

## Set 구현체 비교

```
┌─────────────────┬──────────────┬────────────────┬──────────────┐
│    구현체        │  순서 보장    │   시간 복잡도   │    용도      │
├─────────────────┼──────────────┼────────────────┼──────────────┤
│ HashSet         │ ❌ 없음      │ O(1)           │ 기본, 성능   │
│ LinkedHashSet   │ ✅ 삽입 순서 │ O(1)           │ 순서 필요시  │
│ TreeSet         │ ✅ 정렬 순서 │ O(log n)       │ 정렬, 범위   │
└─────────────────┴──────────────┴────────────────┴──────────────┘
```

## 불변성 (Immutability)

```kotlin
// 불변 Set (읽기 전용)
val immutableSet: Set<Int> = setOf(1, 2, 3)
// immutableSet.add(4)  // 컴파일 에러!

// 가변 Set (읽기/쓰기)
val mutableSet: MutableSet<Int> = mutableSetOf(1, 2, 3)
mutableSet.add(4)  // OK
```

## Set 생성 방법

```kotlin
// 1. 불변 Set
val set1 = setOf(1, 2, 3)
val emptySet = emptySet<String>()

// 2. 가변 Set
val set2 = mutableSetOf(1, 2, 3)
val hashSet = hashSetOf("a", "b", "c")
val linkedSet = linkedSetOf("a", "b", "c")  // 순서 유지
val sortedSet = sortedSetOf(3, 1, 2)        // 정렬됨: [1, 2, 3]

// 3. 변환
val listToSet = listOf(1, 2, 2, 3).toSet()  // 중복 제거: {1, 2, 3}
```

## 핵심 연산

### 요소 확인

```kotlin
val fruits = setOf("apple", "banana", "cherry")

fruits.contains("apple")      // true
"banana" in fruits            // true (in 연산자)
fruits.containsAll(setOf("apple", "banana"))  // true
```

### 요소 추가/제거 (MutableSet)

```kotlin
val numbers = mutableSetOf(1, 2, 3)

// 추가
numbers.add(4)              // true (추가됨)
numbers.add(3)              // false (이미 존재)
numbers.addAll(setOf(5, 6))

// 제거
numbers.remove(1)           // true (제거됨)
numbers.remove(100)         // false (존재하지 않음)
numbers.removeIf { it > 4 } // 조건부 제거
numbers.clear()             // 전체 삭제
```

## 집합 연산

Set의 핵심 기능인 집합 연산입니다.

```
A = {1, 2, 3, 4, 5}
B = {4, 5, 6, 7, 8}

합집합 (Union):     A ∪ B = {1, 2, 3, 4, 5, 6, 7, 8}
교집합 (Intersect): A ∩ B = {4, 5}
차집합 (Subtract):  A - B = {1, 2, 3}
                    B - A = {6, 7, 8}
```

### 코드 예제

```kotlin
val setA = setOf(1, 2, 3, 4, 5)
val setB = setOf(4, 5, 6, 7, 8)

// 합집합 (Union)
val union = setA union setB        // {1, 2, 3, 4, 5, 6, 7, 8}
val union2 = setA + setB           // 동일

// 교집합 (Intersect)
val intersect = setA intersect setB  // {4, 5}

// 차집합 (Subtract)
val subtract = setA subtract setB    // {1, 2, 3}
val subtract2 = setA - setB          // 동일

// 대칭 차집합 (어느 한쪽에만 있는 요소)
val symmetric = (setA - setB) union (setB - setA)  // {1, 2, 3, 6, 7, 8}
```

## 실무 활용 패턴

### 1. 중복 제거

```kotlin
// 기본 중복 제거
val names = listOf("Alice", "Bob", "Alice", "Charlie", "Bob")
val uniqueNames = names.toSet()  // {Alice, Bob, Charlie}

// 대소문자 무시 중복 제거
val emails = listOf("A@ex.com", "a@ex.com", "B@ex.com")
val uniqueEmails = emails.map { it.lowercase() }.toSet()

// 특정 필드 기준 중복 제거
data class User(val id: Long, val email: String)
val users = listOf(User(1, "a@ex.com"), User(2, "a@ex.com"))
val uniqueByEmail = users.distinctBy { it.email }
```

### 2. 멤버십 검사 (빠른 조회)

```kotlin
// 블랙리스트 검사 - Set은 O(1) 조회
val blacklist = setOf("spam.com", "malware.net")

fun isSafeDomain(email: String): Boolean {
    val domain = email.substringAfter("@")
    return domain !in blacklist
}

// 권한 검사
val userPermissions = setOf("READ", "WRITE")
val required = setOf("READ", "WRITE")
val hasPermission = userPermissions.containsAll(required)
```

### 3. 변경 감지 (Diff)

```kotlin
fun detectChanges(before: Set<String>, after: Set<String>): Triple<Set<String>, Set<String>, Set<String>> {
    val added = after - before
    val removed = before - after
    val unchanged = before intersect after
    return Triple(added, removed, unchanged)
}

val old = setOf("feat1", "feat2", "legacy")
val new = setOf("feat1", "feat2", "feat3")
val (added, removed, unchanged) = detectChanges(old, new)
// added: {feat3}, removed: {legacy}, unchanged: {feat1, feat2}
```

### 4. 태그/카테고리 필터링

```kotlin
data class Product(val name: String, val tags: Set<String>)

val products = listOf(
    Product("Laptop", setOf("tech", "work")),
    Product("Phone", setOf("tech", "mobile")),
    Product("Desk", setOf("furniture", "work"))
)

// 특정 태그가 있는 상품
val techProducts = products.filter { "tech" in it.tags }

// 모든 태그를 가진 상품
val workTechProducts = products.filter {
    it.tags.containsAll(setOf("tech", "work"))
}

// 하나라도 태그가 있는 상품
val anyMatch = products.filter {
    it.tags.any { tag -> tag in setOf("mobile", "furniture") }
}
```

## 성능 가이드

### 언제 Set을 사용하는가?

| 상황 | Set 사용 | List 사용 |
|------|---------|----------|
| 중복 제거 필요 | ✅ | ❌ |
| 빈번한 포함 여부 검사 | ✅ | ❌ |
| 집합 연산 필요 | ✅ | ❌ |
| 순서/인덱스 접근 필요 | ❌ | ✅ |
| 중복 허용 필요 | ❌ | ✅ |

### 구현체 선택 가이드

```kotlin
// 기본 선택: HashSet (가장 빠름)
val defaultSet = hashSetOf<String>()

// 삽입 순서가 중요할 때: LinkedHashSet
val orderedSet = linkedSetOf<String>()

// 정렬이 필요할 때: TreeSet
val sortedSet = sortedSetOf<String>()

// 범위 쿼리가 필요할 때: TreeSet + NavigableSet
val navigable = sortedSetOf(1, 3, 5, 7, 9) as java.util.NavigableSet
navigable.ceiling(4)  // 5 (4 이상 최소값)
navigable.floor(4)    // 3 (4 이하 최대값)
navigable.subSet(3, true, 7, true)  // {3, 5, 7}
```

## 주의사항

### 1. equals/hashCode 일관성

```kotlin
// data class는 자동 생성됨
data class User(val id: Long, val name: String)

// 일반 class는 직접 구현 필요
class Email(val value: String) {
    override fun equals(other: Any?): Boolean {
        if (other !is Email) return false
        return value.equals(other.value, ignoreCase = true)
    }

    override fun hashCode(): Int {
        return value.lowercase().hashCode()  // equals와 일관되게!
    }
}
```

### 2. 불변성 보장

```kotlin
class Repository {
    private val items = mutableSetOf<String>()

    // 잘못된 방법: 내부 상태 노출
    // fun getAll(): MutableSet<String> = items

    // 올바른 방법: 복사본 반환
    fun getAll(): Set<String> = items.toSet()
}
```

### 3. MutableSet 수정 중 반복 금지

```kotlin
val numbers = mutableSetOf(1, 2, 3, 4, 5)

// 잘못된 방법: ConcurrentModificationException 발생 가능
// for (n in numbers) {
//     if (n > 3) numbers.remove(n)
// }

// 올바른 방법 1: removeIf 사용
numbers.removeIf { it > 3 }

// 올바른 방법 2: 복사본으로 반복
numbers.toSet().forEach { n ->
    if (n > 3) numbers.remove(n)
}
```

## 관련 테스트 파일

- [SetTest.kt](./SetTest.kt) - 모든 Set 연산의 실행 가능한 테스트 예제

## 다음 학습 주제

- [ ] List - 순서가 있는 컬렉션
- [ ] Map - 키-값 쌍 컬렉션
- [ ] Sequence - 지연 평가 컬렉션
- [ ] Array - 고정 크기 배열
