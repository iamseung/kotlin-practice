package com.example.kotlin.practice

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.assertj.core.api.Assertions.assertThat

/**
 * associateBy vs groupBy 차이점 이해하기
 *
 * associateBy: 컬렉션을 Map<K, V>로 변환 (키는 고유, 1:1 매핑)
 * groupBy: 컬렉션을 Map<K, List<V>>로 변환 (키로 그룹화, 1:N 매핑)
 */
class AssociateByGroupByTest {

    data class User(
        val id: Long,
        val name: String,
        val age: Int,
        val city: String
    )

    data class Order(
        val id: Long,
        val userId: Long,
        val amount: Int,
        val status: String
    )

    // 테스트 데이터
    private val users = listOf(
        User(1L, "김철수", 25, "서울"),
        User(2L, "이영희", 30, "부산"),
        User(3L, "박민수", 25, "서울"),
        User(4L, "정지원", 35, "대구"),
        User(5L, "최상욱", 30, "서울")
    )

    private val orders = listOf(
        Order(1L, 1L, 10000, "COMPLETED"),
        Order(2L, 1L, 20000, "COMPLETED"),
        Order(3L, 1L, 15000, "CANCELLED"),
        Order(4L, 2L, 30000, "COMPLETED"),
        Order(5L, 3L, 25000, "PENDING"),
        Order(6L, 3L, 40000, "COMPLETED"),
        Order(7L, 4L, 50000, "COMPLETED")
    )

    // ============================================
    // 1. associateBy - 고유한 키로 Map 생성
    // ============================================

    @Test
    @DisplayName("associateBy: ID로 User 맵 생성 (키는 고유)")
    fun `associateBy는 고유한 키로 1대1 매핑한다`() {
        // ID를 키로 하는 Map<Long, User> 생성
        val userMap: Map<Long, User> = users.associateBy { it.id }

        println("=== associateBy 결과 ===")
        userMap.forEach { (id, user) ->
            println("ID: $id -> ${user.name}")
        }

        // 검증
        assertThat(userMap).hasSize(5)
        assertThat(userMap[1L]?.name).isEqualTo("김철수")
        assertThat(userMap[3L]?.age).isEqualTo(25)

        // Map<K, V> 형태 - 각 키는 하나의 값만 가짐
        assertThat(userMap[1L]).isInstanceOf(User::class.java)
    }

    @Test
    @DisplayName("associateBy: 키가 중복되면 마지막 값으로 덮어쓰기")
    fun `associateBy는 중복 키 발생시 마지막 값만 유지한다`() {
        // city를 키로 사용 (중복 가능)
        val cityMap: Map<String, User> = users.associateBy { it.city }

        println("\n=== associateBy (중복 키) 결과 ===")
        cityMap.forEach { (city, user) ->
            println("도시: $city -> ${user.name} (나이: ${user.age})")
        }

        // "서울"에 3명이 있지만 마지막 사람만 남음
        assertThat(cityMap).hasSize(3) // 서울, 부산, 대구
        assertThat(cityMap["서울"]?.name).isEqualTo("최상욱") // 마지막 서울 사람

        // ⚠️ 김철수, 박민수는 덮어써짐!
    }

    @Test
    @DisplayName("associateBy with valueSelector: 키와 값을 모두 변환")
    fun `associateBy는 키와 값을 동시에 변환할 수 있다`() {
        // ID를 키로, 이름을 값으로 하는 Map<Long, String>
        val nameMap: Map<Long, String> = users.associateBy(
            keySelector = { it.id },
            valueTransform = { it.name }
        )

        println("\n=== associateBy with valueSelector ===")
        nameMap.forEach { (id, name) ->
            println("ID: $id -> 이름: $name")
        }

        assertThat(nameMap).hasSize(5)
        assertThat(nameMap[1L]).isEqualTo("김철수")
        assertThat(nameMap[2L]).isEqualTo("이영희")
    }

    // ============================================
    // 2. groupBy - 같은 키로 그룹화
    // ============================================

    @Test
    @DisplayName("groupBy: 나이로 User 그룹화 (1:N 매핑)")
    fun `groupBy는 같은 키를 가진 값들을 리스트로 그룹화한다`() {
        // 나이를 키로 그룹화 -> Map<Int, List<User>>
        val ageGroups: Map<Int, List<User>> = users.groupBy { it.age }

        println("\n=== groupBy 결과 ===")
        ageGroups.forEach { (age, userList) ->
            println("나이 $age: ${userList.map { it.name }}")
        }

        // 검증
        assertThat(ageGroups).hasSize(3) // 25, 30, 35
        assertThat(ageGroups[25]).hasSize(2) // 김철수, 박민수
        assertThat(ageGroups[30]).hasSize(2) // 이영희, 최상욱
        assertThat(ageGroups[35]).hasSize(1) // 정지원

        // Map<K, List<V>> 형태 - 각 키는 리스트를 가짐
        assertThat(ageGroups[25]).isInstanceOf(List::class.java)
        assertThat(ageGroups[25]?.get(0)).isInstanceOf(User::class.java)
    }

    @Test
    @DisplayName("groupBy: 도시별 User 그룹화")
    fun `groupBy는 모든 중복 값을 리스트로 보존한다`() {
        // 도시를 키로 그룹화
        val cityGroups: Map<String, List<User>> = users.groupBy { it.city }

        println("\n=== groupBy (도시별) 결과 ===")
        cityGroups.forEach { (city, userList) ->
            println("$city: ${userList.map { "${it.name}(${it.age}세)" }}")
        }

        // 서울에 3명 모두 보존됨 (associateBy와 차이!)
        assertThat(cityGroups["서울"]).hasSize(3)
        assertThat(cityGroups["서울"]?.map { it.name })
            .containsExactlyInAnyOrder("김철수", "박민수", "최상욱")

        assertThat(cityGroups["부산"]).hasSize(1)
        assertThat(cityGroups["대구"]).hasSize(1)
    }

    @Test
    @DisplayName("groupBy with valueTransform: 값을 변환하여 그룹화")
    fun `groupBy는 값을 변환하면서 그룹화할 수 있다`() {
        // 나이를 키로, 이름만 추출하여 그룹화
        val ageNameGroups: Map<Int, List<String>> = users.groupBy(
            keySelector = { it.age },
            valueTransform = { it.name }
        )

        println("\n=== groupBy with valueTransform ===")
        ageNameGroups.forEach { (age, names) ->
            println("나이 $age: $names")
        }

        assertThat(ageNameGroups[25]).containsExactly("김철수", "박민수")
        assertThat(ageNameGroups[30]).containsExactly("이영희", "최상욱")
    }

    // ============================================
    // 3. 실전 활용: 주문 데이터 처리
    // ============================================

    @Test
    @DisplayName("실전: userId로 주문 그룹화 (한 사용자의 모든 주문)")
    fun `실전예제_사용자별_주문목록_조회`() {
        // userId로 주문 그룹화
        val ordersByUser: Map<Long, List<Order>> = orders.groupBy { it.userId }

        println("\n=== 사용자별 주문 목록 ===")
        ordersByUser.forEach { (userId, orderList) ->
            val totalAmount = orderList.sumOf { it.amount }
            println("사용자 $userId: 주문 ${orderList.size}건, 총액 ${totalAmount}원")
            orderList.forEach { order ->
                println("  - 주문 ${order.id}: ${order.amount}원 (${order.status})")
            }
        }

        // 검증
        assertThat(ordersByUser[1L]).hasSize(3) // 김철수 3건
        assertThat(ordersByUser[2L]).hasSize(1) // 이영희 1건
        assertThat(ordersByUser[3L]).hasSize(2) // 박민수 2건
    }

    @Test
    @DisplayName("실전: 상태별 주문 그룹화 및 통계")
    fun `실전예제_주문상태별_통계`() {
        // 상태별로 주문 그룹화
        val ordersByStatus: Map<String, List<Order>> = orders.groupBy { it.status }

        println("\n=== 주문 상태별 통계 ===")
        ordersByStatus.forEach { (status, orderList) ->
            val count = orderList.size
            val totalAmount = orderList.sumOf { it.amount }
            val avgAmount = totalAmount / count
            println("[$status] 주문 ${count}건, 총액 ${totalAmount}원, 평균 ${avgAmount}원")
        }

        // 검증
        assertThat(ordersByStatus["COMPLETED"]).hasSize(5)
        assertThat(ordersByStatus["PENDING"]).hasSize(1)
        assertThat(ordersByStatus["CANCELLED"]).hasSize(1)
    }

    @Test
    @DisplayName("실전: 주문 ID로 빠른 조회를 위한 Map 생성")
    fun `실전예제_주문ID로_빠른_조회`() {
        // 주문 ID를 키로 하는 맵 생성 (O(1) 조회)
        val orderMap: Map<Long, Order> = orders.associateBy { it.id }

        // 특정 주문 빠르게 조회
        val order3 = orderMap[3L]
        println("\n=== 주문 조회 (O(1)) ===")
        println("주문 3번: ${order3?.amount}원, ${order3?.status}")

        // 검증
        assertThat(orderMap).hasSize(7)
        assertThat(orderMap[3L]?.amount).isEqualTo(15000)
        assertThat(orderMap[3L]?.status).isEqualTo("CANCELLED")

        // List에서 find() 사용 시 O(n)
        val foundOrder = orders.find { it.id == 3L }
        assertThat(foundOrder).isEqualTo(orderMap[3L])
    }

    @Test
    @DisplayName("실전: User와 Order 조인하여 사용자별 주문 정보")
    fun `실전예제_사용자와_주문_조인`() {
        // 1단계: userId로 주문 그룹화
        val ordersByUser: Map<Long, List<Order>> = orders.groupBy { it.userId }

        // 2단계: userId로 User 맵 생성
        val userMap: Map<Long, User> = users.associateBy { it.id }

        println("\n=== 사용자-주문 조인 결과 ===")
        ordersByUser.forEach { (userId, orderList) ->
            val user = userMap[userId]
            if (user != null) {
                val totalAmount = orderList.sumOf { it.amount }
                val completedCount = orderList.count { it.status == "COMPLETED" }
                println("${user.name}(${user.city})")
                println("  총 주문: ${orderList.size}건")
                println("  완료된 주문: ${completedCount}건")
                println("  총 주문금액: ${totalAmount}원")
            }
        }

        // 검증: 김철수(userId=1L)의 주문 통계
        val user1Orders = ordersByUser[1L]!!
        val user1 = userMap[1L]!!
        assertThat(user1.name).isEqualTo("김철수")
        assertThat(user1Orders).hasSize(3)
        assertThat(user1Orders.sumOf { it.amount }).isEqualTo(45000)
    }

    // ============================================
    // 4. 핵심 차이점 비교
    // ============================================

    @Test
    @DisplayName("핵심 차이: associateBy vs groupBy")
    fun `associateBy와_groupBy의_차이점_비교`() {
        println("\n=== associateBy vs groupBy 비교 ===\n")

        // associateBy: Map<String, User>
        val cityMapAssociate = users.associateBy { it.city }
        println("associateBy 결과 타입: Map<String, User>")
        println("서울: ${cityMapAssociate["서울"]?.name} (마지막 값만 유지)")
        println("크기: ${cityMapAssociate.size}개 도시\n")

        // groupBy: Map<String, List<User>>
        val cityMapGroup = users.groupBy { it.city }
        println("groupBy 결과 타입: Map<String, List<User>>")
        println("서울: ${cityMapGroup["서울"]?.map { it.name }} (모든 값 유지)")
        println("크기: ${cityMapGroup.size}개 도시")

        // 차이점 검증
        assertThat(cityMapAssociate["서울"]).isInstanceOf(User::class.java)
        assertThat(cityMapGroup["서울"]).isInstanceOf(List::class.java)
        assertThat(cityMapGroup["서울"]).hasSize(3)
    }

    // ============================================
    // 5. 언제 무엇을 사용할까?
    // ============================================

    @Test
    @DisplayName("사용 시나리오 가이드")
    fun `언제_어떤_함수를_사용할까`() {
        println("\n=== 사용 시나리오 가이드 ===\n")

        println("✅ associateBy를 사용하는 경우:")
        println("  1. ID로 엔티티를 빠르게 조회하고 싶을 때")
        println("  2. 고유한 키로 1:1 매핑이 필요할 때")
        println("  3. O(1) 조회 성능이 중요할 때")
        println("  예: Map<UserId, User>, Map<OrderId, Order>\n")

        println("✅ groupBy를 사용하는 경우:")
        println("  1. 같은 속성을 가진 데이터를 그룹화할 때")
        println("  2. 카테고리별 통계를 내고 싶을 때")
        println("  3. 중복 키에 대한 모든 값이 필요할 때")
        println("  예: 상태별 주문 목록, 도시별 사용자 목록\n")

        println("⚠️ 주의:")
        println("  - associateBy는 중복 키 발생 시 마지막 값만 유지")
        println("  - groupBy는 중복 키의 모든 값을 List로 보존")
    }
}
