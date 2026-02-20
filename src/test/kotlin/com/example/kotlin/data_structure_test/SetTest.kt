package com.example.kotlin.data_structure_test

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.TreeSet

/**
 * Set (집합) - 중복을 허용하지 않는 컬렉션
 *
 * 핵심 개념:
 * 1. 중복 불허: 동일한 요소는 하나만 저장됨
 * 2. 순서:
 *    - HashSet: 순서 보장 안함 (O(1) 접근)
 *    - LinkedHashSet: 삽입 순서 유지
 *    - TreeSet: 정렬된 순서 유지 (O(log n) 접근)
 * 3. 불변성:
 *    - Set<T>: 읽기 전용 (Immutable)
 *    - MutableSet<T>: 읽기/쓰기 가능 (Mutable)
 * 4. 집합 연산: union, intersect, subtract
 */
class SetTest {

    // 테스트용 도메인 클래스들
    data class User(val id: Long, val name: String, val email: String)
    data class Tag(val id: Long, val name: String)
    data class Product(val id: Long, val name: String, val tags: Set<Tag>)

    // =========================================================================
    // 1. Set 생성 방법
    // =========================================================================

    /**
     * 예제 1: Set 생성 - 다양한 방법
     */
    @Test
    fun `Set 생성 - 다양한 방법`() {
        // 1. setOf - 불변 Set 생성
        val immutableSet = setOf(1, 2, 3, 4, 5)
        assertEquals(5, immutableSet.size)
        // immutableSet.add(6) // 컴파일 에러! - 불변이라 추가 불가

        // 2. mutableSetOf - 가변 Set 생성
        val mutableSet = mutableSetOf(1, 2, 3)
        mutableSet.add(4)
        mutableSet.add(5)
        assertEquals(5, mutableSet.size)

        // 3. hashSetOf - HashSet 생성 (순서 보장 안함)
        val hashSet = hashSetOf("apple", "banana", "cherry")
        assertTrue(hashSet.contains("apple"))

        // 4. linkedSetOf - LinkedHashSet 생성 (삽입 순서 유지)
        val linkedSet = linkedSetOf("first", "second", "third")
        assertEquals("first", linkedSet.first())
        assertEquals("third", linkedSet.last())

        // 5. sortedSetOf - TreeSet 생성 (정렬된 순서)
        val sortedSet = sortedSetOf(3, 1, 4, 1, 5, 9, 2, 6)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 9), sortedSet.toList())

        // 6. 빈 Set 생성
        val emptySet: Set<String> = emptySet()
        assertTrue(emptySet.isEmpty())

        // 7. 컬렉션에서 Set으로 변환
        val list = listOf(1, 2, 2, 3, 3, 3, 4)
        val setFromList = list.toSet()
        assertEquals(4, setFromList.size) // 중복 제거됨
    }

    /**
     * 예제 2: Set의 중복 제거 특성
     */
    @Test
    fun `Set - 중복 제거 특성`() {
        // 숫자 중복 제거
        val numbers = setOf(1, 2, 2, 3, 3, 3, 4, 4, 4, 4)
        assertEquals(4, numbers.size)
        assertEquals(setOf(1, 2, 3, 4), numbers)

        // 문자열 중복 제거
        val words = setOf("apple", "Apple", "APPLE", "apple")
        assertEquals(3, words.size) // 대소문자 구분함

        // 객체 중복 제거 (data class의 equals 사용)
        val user1 = User(1, "Alice", "alice@example.com")
        val user2 = User(1, "Alice", "alice@example.com") // user1과 동일
        val user3 = User(2, "Bob", "bob@example.com")

        val users = setOf(user1, user2, user3)
        assertEquals(2, users.size) // user1과 user2는 같으므로 2개

        // MutableSet에 중복 추가 시도
        val mutableUsers = mutableSetOf(user1)
        val added = mutableUsers.add(user2) // user1과 동일하므로 추가 안됨
        assertFalse(added)
        assertEquals(1, mutableUsers.size)
    }

    // =========================================================================
    // 2. Set 기본 연산
    // =========================================================================

    /**
     * 예제 3: Set 요소 접근 및 확인
     */
    @Test
    fun `Set - 요소 접근 및 확인`() {
        val fruits = setOf("apple", "banana", "cherry", "date")

        // 포함 여부 확인
        assertTrue(fruits.contains("apple"))
        assertTrue("banana" in fruits) // in 연산자 사용
        assertFalse("grape" in fruits)

        // 여러 요소 포함 여부
        assertTrue(fruits.containsAll(setOf("apple", "banana")))
        assertFalse(fruits.containsAll(setOf("apple", "grape")))

        // 첫 번째, 마지막 요소 (LinkedHashSet, TreeSet에서 유의미)
        val orderedFruits = linkedSetOf("apple", "banana", "cherry")
        assertEquals("apple", orderedFruits.first())
        assertEquals("cherry", orderedFruits.last())

        // 빈 Set 안전 처리
        val emptyFruits = emptySet<String>()
        assertNull(emptyFruits.firstOrNull())
        assertEquals("default", emptyFruits.firstOrNull() ?: "default")
    }

    /**
     * 예제 4: MutableSet 수정 연산
     */
    @Test
    fun `MutableSet - 수정 연산`() {
        val numbers = mutableSetOf(1, 2, 3)

        // 단일 요소 추가
        assertTrue(numbers.add(4)) // 성공
        assertFalse(numbers.add(3)) // 이미 존재하므로 실패
        assertEquals(setOf(1, 2, 3, 4), numbers)

        // 여러 요소 추가
        numbers.addAll(setOf(5, 6, 7))
        assertEquals(7, numbers.size)

        // 단일 요소 제거
        assertTrue(numbers.remove(1)) // 성공
        assertFalse(numbers.remove(100)) // 존재하지 않으므로 실패
        assertEquals(6, numbers.size)

        // 여러 요소 제거
        numbers.removeAll(setOf(2, 3, 4))
        assertEquals(setOf(5, 6, 7), numbers)

        // 조건에 맞는 요소 제거
        numbers.addAll(setOf(8, 9, 10))
        numbers.removeIf { it > 7 } // 7보다 큰 요소 제거
        assertEquals(setOf(5, 6, 7), numbers)

        // 특정 요소만 유지
        numbers.addAll(setOf(1, 2, 3, 8, 9))
        numbers.retainAll(setOf(1, 2, 3, 4, 5)) // 교집합만 유지
        assertEquals(setOf(1, 2, 3, 5), numbers)

        // 전체 삭제
        numbers.clear()
        assertTrue(numbers.isEmpty())
    }

    // =========================================================================
    // 3. 집합 연산 (Set Operations)
    // =========================================================================

    /**
     * 예제 5: 합집합 (Union)
     */
    @Test
    fun `Set - 합집합 Union`() {
        val set1 = setOf(1, 2, 3, 4, 5)
        val set2 = setOf(4, 5, 6, 7, 8)

        // union 함수 사용
        val union = set1.union(set2)
        assertEquals(setOf(1, 2, 3, 4, 5, 6, 7, 8), union)

        // + 연산자 사용 (union과 동일)
        val unionWithPlus = set1 + set2
        assertEquals(union, unionWithPlus)

        // 실용 예제: 여러 팀의 멤버 합치기
        val teamA = setOf("Alice", "Bob", "Charlie")
        val teamB = setOf("Charlie", "David", "Eve")
        val allMembers = teamA union teamB
        assertEquals(5, allMembers.size)
        assertTrue(allMembers.containsAll(setOf("Alice", "Bob", "Charlie", "David", "Eve")))
    }

    /**
     * 예제 6: 교집합 (Intersect)
     */
    @Test
    fun `Set - 교집합 Intersect`() {
        val set1 = setOf(1, 2, 3, 4, 5)
        val set2 = setOf(4, 5, 6, 7, 8)

        // intersect 함수 사용
        val intersection = set1.intersect(set2)
        assertEquals(setOf(4, 5), intersection)

        // 빈 교집합
        val set3 = setOf(10, 11, 12)
        val emptyIntersection = set1.intersect(set3)
        assertTrue(emptyIntersection.isEmpty())

        // 실용 예제: 공통 관심사 찾기
        val aliceInterests = setOf("Kotlin", "Java", "Python", "Music")
        val bobInterests = setOf("Python", "JavaScript", "Music", "Gaming")
        val commonInterests = aliceInterests intersect bobInterests
        assertEquals(setOf("Python", "Music"), commonInterests)
    }

    /**
     * 예제 7: 차집합 (Subtract)
     */
    @Test
    fun `Set - 차집합 Subtract`() {
        val set1 = setOf(1, 2, 3, 4, 5)
        val set2 = setOf(4, 5, 6, 7, 8)

        // subtract 함수 사용
        val difference = set1.subtract(set2)
        assertEquals(setOf(1, 2, 3), difference)

        // - 연산자 사용 (subtract와 동일)
        val differenceWithMinus = set1 - set2
        assertEquals(difference, differenceWithMinus)

        // 반대 방향 차집합
        val reverseDifference = set2 - set1
        assertEquals(setOf(6, 7, 8), reverseDifference)

        // 실용 예제: 완료되지 않은 작업 찾기
        val allTasks = setOf("Task1", "Task2", "Task3", "Task4", "Task5")
        val completedTasks = setOf("Task1", "Task3")
        val remainingTasks = allTasks - completedTasks
        assertEquals(setOf("Task2", "Task4", "Task5"), remainingTasks)
    }

    /**
     * 예제 8: 대칭 차집합 (Symmetric Difference)
     * 어느 한쪽에만 있는 요소들
     */
    @Test
    fun `Set - 대칭 차집합`() {
        val set1 = setOf(1, 2, 3, 4, 5)
        val set2 = setOf(4, 5, 6, 7, 8)

        // 대칭 차집합 = (A - B) ∪ (B - A)
        val symmetricDifference = (set1 - set2) union (set2 - set1)
        assertEquals(setOf(1, 2, 3, 6, 7, 8), symmetricDifference)

        // 또는 합집합에서 교집합 빼기
        val alternative = (set1 union set2) - (set1 intersect set2)
        assertEquals(symmetricDifference, alternative)
    }

    // =========================================================================
    // 4. Set 변환 및 고급 연산
    // =========================================================================

    /**
     * 예제 9: Set 변환 연산
     */
    @Test
    fun `Set - 변환 연산`() {
        val numbers = setOf(1, 2, 3, 4, 5)

        // map - 각 요소를 변환 (결과는 List)
        val doubled = numbers.map { it * 2 }
        assertEquals(listOf(2, 4, 6, 8, 10), doubled)

        // map 후 Set으로 변환
        val doubledSet = numbers.map { it * 2 }.toSet()
        assertEquals(setOf(2, 4, 6, 8, 10), doubledSet)

        // filter - 조건에 맞는 요소만 선택
        val evenNumbers = numbers.filter { it % 2 == 0 }.toSet()
        assertEquals(setOf(2, 4), evenNumbers)

        // filterNot - 조건에 맞지 않는 요소 선택
        val oddNumbers = numbers.filterNot { it % 2 == 0 }.toSet()
        assertEquals(setOf(1, 3, 5), oddNumbers)

        // flatMap - 중첩 컬렉션 평탄화
        val nestedSets = setOf(setOf(1, 2), setOf(3, 4), setOf(5))
        val flattened = nestedSets.flatMap { it }.toSet()
        assertEquals(setOf(1, 2, 3, 4, 5), flattened)

        // partition - 조건에 따라 두 Set으로 분리
        val (even, odd) = numbers.partition { it % 2 == 0 }
        assertEquals(listOf(2, 4), even)
        assertEquals(listOf(1, 3, 5), odd)
    }

    /**
     * 예제 10: Set 집계 연산
     */
    @Test
    fun `Set - 집계 연산`() {
        val numbers = setOf(5, 2, 8, 1, 9, 3)

        // 기본 집계
        assertEquals(6, numbers.size)
        assertEquals(6, numbers.count())
        assertEquals(28, numbers.sum())
        assertEquals(4.666666666666667, numbers.average(), 0.0001)
        assertEquals(1, numbers.minOrNull())
        assertEquals(9, numbers.maxOrNull())

        // 조건부 집계
        assertEquals(3, numbers.count { it > 4 })
        assertEquals(5, numbers.filter { it > 4 }.sumOf { it })

        // reduce - 누적 연산
        val product = numbers.reduce { acc, n -> acc * n }
        assertEquals(2160, product) // 5 * 2 * 8 * 1 * 9 * 3

        // fold - 초기값과 함께 누적 연산
        val sumWithInitial = numbers.fold(100) { acc, n -> acc + n }
        assertEquals(128, sumWithInitial) // 100 + 28

        // groupBy - 그룹화
        val grouped = numbers.groupBy { if (it % 2 == 0) "even" else "odd" }
        assertEquals(setOf(2, 8), grouped["even"]?.toSet())
        assertEquals(setOf(5, 1, 9, 3), grouped["odd"]?.toSet())
    }

    /**
     * 예제 11: Set 정렬
     */
    @Test
    fun `Set - 정렬`() {
        val numbers = setOf(5, 2, 8, 1, 9, 3)

        // sorted - 오름차순 정렬 (결과는 List)
        val sortedAsc = numbers.sorted()
        assertEquals(listOf(1, 2, 3, 5, 8, 9), sortedAsc)

        // sortedDescending - 내림차순 정렬
        val sortedDesc = numbers.sortedDescending()
        assertEquals(listOf(9, 8, 5, 3, 2, 1), sortedDesc)

        // sortedSetOf - 항상 정렬된 상태 유지
        val sortedSet = sortedSetOf(5, 2, 8, 1, 9, 3)
        assertEquals(listOf(1, 2, 3, 5, 8, 9), sortedSet.toList())
        (sortedSet as TreeSet).add(4)
        assertEquals(listOf(1, 2, 3, 4, 5, 8, 9), sortedSet.toList())

        // sortedBy - 커스텀 기준 정렬
        val users = setOf(
            User(3, "Charlie", "c@example.com"),
            User(1, "Alice", "a@example.com"),
            User(2, "Bob", "b@example.com")
        )
        val sortedByName = users.sortedBy { it.name }
        assertEquals("Alice", sortedByName.first().name)
        assertEquals("Charlie", sortedByName.last().name)
    }

    // =========================================================================
    // 5. 실무 활용 예제
    // =========================================================================

    /**
     * 예제 12: 중복 제거 실무 패턴
     */
    @Test
    fun `실무 - 중복 제거 패턴`() {
        // 이메일 중복 제거
        val emails = listOf(
            "alice@example.com",
            "bob@example.com",
            "ALICE@example.com", // 대소문자 차이
            "alice@example.com", // 완전 중복
            "charlie@example.com"
        )

        // 대소문자 무시하고 중복 제거
        val uniqueEmails = emails.map { it.lowercase() }.toSet()
        assertEquals(3, uniqueEmails.size)

        // 특정 필드 기준으로 중복 제거
        val usersWithDuplicates = listOf(
            User(1, "Alice", "alice@example.com"),
            User(2, "Bob", "bob@example.com"),
            User(3, "Alice Clone", "alice@example.com"), // 같은 이메일
            User(4, "Charlie", "charlie@example.com")
        )

        // distinctBy - 특정 필드 기준 중복 제거
        val uniqueByEmail = usersWithDuplicates.distinctBy { it.email }
        assertEquals(3, uniqueByEmail.size)

        // associateBy로 마지막 값 유지
        val emailToUser = usersWithDuplicates.associateBy { it.email }
        assertEquals(3, emailToUser.size)
        assertEquals("Alice Clone", emailToUser["alice@example.com"]?.name)
    }

    /**
     * 예제 13: 멤버십 검사 최적화
     */
    @Test
    fun `실무 - 멤버십 검사 최적화`() {
        // 블랙리스트 검사
        val blacklistedDomains = setOf(
            "spam.com",
            "malware.net",
            "phishing.org"
        )

        fun isEmailSafe(email: String): Boolean {
            val domain = email.substringAfter("@")
            return domain !in blacklistedDomains
        }

        assertTrue(isEmailSafe("user@gmail.com"))
        assertFalse(isEmailSafe("hacker@spam.com"))

        // 권한 검사
        val userPermissions = setOf("READ", "WRITE", "DELETE")
        val requiredPermissions = setOf("READ", "WRITE")

        // 필요한 모든 권한이 있는지 확인
        val hasAllPermissions = userPermissions.containsAll(requiredPermissions)
        assertTrue(hasAllPermissions)

        // 특정 권한이 하나라도 있는지 확인
        val dangerousPermissions = setOf("DELETE", "ADMIN")
        val hasDangerousPermission = userPermissions.any { it in dangerousPermissions }
        assertTrue(hasDangerousPermission)
    }

    /**
     * 예제 14: 태그/카테고리 관리
     */
    @Test
    fun `실무 - 태그 관리 시스템`() {
        val kotlin = Tag(1, "Kotlin")
        val java = Tag(2, "Java")
        val programming = Tag(3, "Programming")
        val backend = Tag(4, "Backend")
        val mobile = Tag(5, "Mobile")

        val product1 = Product(1, "Kotlin Guide", setOf(kotlin, programming, backend))
        val product2 = Product(2, "Java Basics", setOf(java, programming, backend))
        val product3 = Product(3, "Android Dev", setOf(kotlin, mobile, programming))

        // 모든 태그 수집
        val allProducts = listOf(product1, product2, product3)
        val allTags = allProducts.flatMap { it.tags }.toSet()
        assertEquals(5, allTags.size)

        // 특정 태그가 있는 상품 필터링
        fun findByTag(products: List<Product>, tag: Tag): List<Product> {
            return products.filter { tag in it.tags }
        }

        val kotlinProducts = findByTag(allProducts, kotlin)
        assertEquals(2, kotlinProducts.size)

        // 여러 태그 중 하나라도 있는 상품 (OR)
        fun findByAnyTag(products: List<Product>, tags: Set<Tag>): List<Product> {
            return products.filter { product ->
                product.tags.any { it in tags }
            }
        }

        val mobileOrBackend = findByAnyTag(allProducts, setOf(mobile, backend))
        assertEquals(3, mobileOrBackend.size)

        // 모든 태그가 있는 상품 (AND)
        fun findByAllTags(products: List<Product>, tags: Set<Tag>): List<Product> {
            return products.filter { product ->
                product.tags.containsAll(tags)
            }
        }

        val kotlinBackend = findByAllTags(allProducts, setOf(kotlin, backend))
        assertEquals(1, kotlinBackend.size)
        assertEquals("Kotlin Guide", kotlinBackend.first().name)
    }

    /**
     * 예제 15: 변경 감지 (diff)
     */
    @Test
    fun `실무 - 변경 감지`() {
        data class ConfigChange(
            val added: Set<String>,
            val removed: Set<String>,
            val unchanged: Set<String>
        )

        fun detectChanges(before: Set<String>, after: Set<String>): ConfigChange {
            return ConfigChange(
                added = after - before,
                removed = before - after,
                unchanged = before intersect after
            )
        }

        val oldConfig = setOf("feature1", "feature2", "feature3", "legacy")
        val newConfig = setOf("feature1", "feature2", "feature4", "feature5")

        val changes = detectChanges(oldConfig, newConfig)

        assertEquals(setOf("feature4", "feature5"), changes.added)
        assertEquals(setOf("feature3", "legacy"), changes.removed)
        assertEquals(setOf("feature1", "feature2"), changes.unchanged)

        // 변경 로그 생성
        val changeLog = buildString {
            if (changes.added.isNotEmpty()) {
                appendLine("Added: ${changes.added.joinToString()}")
            }
            if (changes.removed.isNotEmpty()) {
                appendLine("Removed: ${changes.removed.joinToString()}")
            }
        }

        assertTrue(changeLog.contains("Added: feature4, feature5"))
        assertTrue(changeLog.contains("Removed: feature3, legacy"))
    }

    /**
     * 예제 16: Set을 활용한 상태 관리
     */
    @Test
    fun `실무 - 선택 상태 관리`() {
        class SelectionManager<T> {
            private val selected = mutableSetOf<T>()

            fun select(item: T): Boolean = selected.add(item)
            fun deselect(item: T): Boolean = selected.remove(item)
            fun toggle(item: T) {
                if (item in selected) selected.remove(item) else selected.add(item)
            }
            fun isSelected(item: T): Boolean = item in selected
            fun selectAll(items: Collection<T>) = selected.addAll(items)
            fun deselectAll() = selected.clear()
            fun getSelected(): Set<T> = selected.toSet() // 불변 복사본 반환
            fun count(): Int = selected.size
        }

        val manager = SelectionManager<Long>()

        // 선택
        assertTrue(manager.select(1))
        assertTrue(manager.select(2))
        assertFalse(manager.select(1)) // 이미 선택됨

        // 확인
        assertTrue(manager.isSelected(1))
        assertFalse(manager.isSelected(3))

        // 토글
        manager.toggle(1) // 선택 해제
        assertFalse(manager.isSelected(1))
        manager.toggle(1) // 다시 선택
        assertTrue(manager.isSelected(1))

        // 전체 선택
        manager.selectAll(listOf(3, 4, 5, 6))
        assertEquals(5, manager.count())

        // 선택 해제
        manager.deselect(6)
        assertEquals(4, manager.count())

        // 전체 해제
        manager.deselectAll()
        assertEquals(0, manager.count())
    }

    // =========================================================================
    // 6. 성능 고려사항
    // =========================================================================

    /**
     * 예제 17: HashSet vs LinkedHashSet vs TreeSet 비교
     */
    @Test
    fun `성능 - Set 구현체 비교`() {
        // HashSet: 가장 빠른 조회 O(1), 순서 보장 없음
        val hashSet = hashSetOf<Int>()
        repeat(5) { hashSet.add(5 - it) } // 5, 4, 3, 2, 1 순서로 추가
        // hashSet.toList()는 순서가 일정하지 않을 수 있음

        // LinkedHashSet: 삽입 순서 유지, HashSet보다 약간 느림
        val linkedSet = linkedSetOf<Int>()
        repeat(5) { linkedSet.add(5 - it) } // 5, 4, 3, 2, 1 순서로 추가
        assertEquals(listOf(5, 4, 3, 2, 1), linkedSet.toList()) // 삽입 순서 유지

        // TreeSet: 항상 정렬 O(log n), 범위 쿼리 가능
        val treeSet = sortedSetOf<Int>()
        repeat(5) { treeSet.add(5 - it) } // 5, 4, 3, 2, 1 순서로 추가
        assertEquals(listOf(1, 2, 3, 4, 5), treeSet.toList()) // 항상 정렬됨

        // TreeSet의 범위 쿼리
        val numbers = sortedSetOf(1, 3, 5, 7, 9, 11, 13, 15)

        // NavigableSet 기능 사용
        val navigableSet = numbers as java.util.NavigableSet<Int>
        assertEquals(5, navigableSet.ceiling(4)) // 4 이상의 최소값
        assertEquals(3, navigableSet.floor(4))   // 4 이하의 최대값
        assertEquals(5, navigableSet.higher(3))  // 3 초과의 최소값
        assertEquals(3, navigableSet.lower(5))   // 5 미만의 최대값

        // 부분 집합
        val subSet = navigableSet.subSet(5, true, 11, true)
        assertEquals(setOf(5, 7, 9, 11), subSet)
    }

    /**
     * 예제 18: List의 contains vs Set의 contains 성능 비교 개념
     */
    @Test
    fun `성능 - 멤버십 검사 효율성`() {
        // List: contains는 O(n) - 처음부터 끝까지 탐색
        val list = (1..10000).toList()

        // Set: contains는 O(1) - 해시 기반 직접 접근
        val set = (1..10000).toSet()

        // 둘 다 같은 결과를 반환
        assertTrue(list.contains(5000))
        assertTrue(set.contains(5000))

        // 실무에서는 반복적인 멤버십 검사가 필요하면 Set 사용
        // 예: 블랙리스트 검사, 권한 확인 등

        // 작은 컬렉션(~10개)에서는 List도 충분히 빠름
        // 큰 컬렉션이나 빈번한 검사에서는 Set이 훨씬 유리
    }

    // =========================================================================
    // 7. 불변성과 방어적 복사
    // =========================================================================

    /**
     * 예제 19: 불변 Set 활용
     */
    @Test
    fun `불변성 - 방어적 복사`() {
        class UserRepository {
            private val users = mutableSetOf<User>()

            fun add(user: User) = users.add(user)

            // 잘못된 방법: 내부 MutableSet을 직접 반환
            // fun getAllUnsafe(): MutableSet<User> = users

            // 올바른 방법 1: 불변 Set으로 반환
            fun getAll(): Set<User> = users.toSet()

            // 올바른 방법 2: 읽기 전용 뷰로 반환
            fun getAllView(): Set<User> = users
        }

        val repository = UserRepository()
        repository.add(User(1, "Alice", "alice@example.com"))
        repository.add(User(2, "Bob", "bob@example.com"))

        // 반환된 Set은 불변 (수정 불가)
        val users = repository.getAll()
        assertEquals(2, users.size)
        // (users as MutableSet).add(...) // 런타임 에러!

        // 원본과 독립적
        repository.add(User(3, "Charlie", "charlie@example.com"))
        assertEquals(2, users.size) // toSet()은 복사본이므로 영향 없음
    }

    /**
     * 예제 20: 커스텀 equals/hashCode와 Set
     */
    @Test
    fun `커스텀 equals hashCode - Set 동작`() {
        // data class는 자동으로 equals/hashCode 구현
        data class Email(val value: String) {
            override fun equals(other: Any?): Boolean {
                if (other !is Email) return false
                // 대소문자 무시 비교
                return value.equals(other.value, ignoreCase = true)
            }

            override fun hashCode(): Int {
                // equals와 일관되게 소문자로 해시
                return value.lowercase().hashCode()
            }
        }

        val email1 = Email("Alice@Example.com")
        val email2 = Email("alice@example.com")
        val email3 = Email("ALICE@EXAMPLE.COM")

        // equals가 true면 같은 요소로 취급
        assertEquals(email1, email2)
        assertEquals(email2, email3)

        val emails = setOf(email1, email2, email3)
        assertEquals(1, emails.size) // 모두 같은 이메일이므로 1개만 저장
    }
}
