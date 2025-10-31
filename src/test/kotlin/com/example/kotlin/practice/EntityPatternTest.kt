package com.example.kotlin.practice

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Kotlin JPA 엔티티 패턴 비교 테스트
 *
 * 패턴 1: 주 생성자 방식 (Primary Constructor) - 권장 ✅
 * 패턴 2: 본문 프로퍼티 방식 (Body Properties) - 비권장
 * 패턴 3: 하이브리드 방식 (Hybrid) - 상황에 따라
 */
class EntityPatternTest {

    // ============================================
    // 패턴 1: 주 생성자 방식 (권장) ✅
    // ============================================

    class Pattern1User(
        val id: Long? = null,
        val email: String,
        val name: String,
        var point: BigDecimal = BigDecimal.ZERO,
        val createdAt: LocalDateTime = LocalDateTime.now()
    ) {
        fun addPoint(amount: BigDecimal) {
            this.point = this.point.add(amount)
        }

        override fun toString() = "User(id=$id, email='$email', name='$name', point=$point)"
    }

    @Test
    @DisplayName("패턴1: 주 생성자 방식 - 간결하고 명확")
    fun `패턴1_주_생성자_방식`() {
        println("\n=== 패턴 1: 주 생성자 방식 ===\n")

        // 1. 기본 생성
        val user1 = Pattern1User(
            email = "kim@example.com",
            name = "김철수"
        )
        println("생성 1: $user1")

        // 2. 기본값 활용
        val user2 = Pattern1User(
            email = "lee@example.com",
            name = "이영희",
            point = BigDecimal(10000)
        )
        println("생성 2: $user2")

        // 3. 명명된 인자로 순서 무관
        val user3 = Pattern1User(
            name = "박민수",
            email = "park@example.com",
            id = 999L
        )
        println("생성 3: $user3")

        // 4. 불변 필드 보장
        // user1.email = "new@email.com"  // ❌ 컴파일 에러! val은 변경 불가

        // 5. 가변 필드는 변경 가능
        user1.addPoint(BigDecimal(5000))
        println("포인트 추가 후: $user1")

        // 검증
        assertThat(user1.email).isEqualTo("kim@example.com")
        assertThat(user1.point).isEqualByComparingTo(BigDecimal(5000))
        assertThat(user2.point).isEqualByComparingTo(BigDecimal(10000))
    }

    // ============================================
    // 패턴 2: 본문 프로퍼티 방식 (비권장)
    // ============================================

    class Pattern2User {
        var id: Long? = null
        lateinit var email: String
        lateinit var name: String
        var point: BigDecimal = BigDecimal.ZERO
        var createdAt: LocalDateTime = LocalDateTime.now()

        // JPA용 기본 생성자
        constructor()

        // 실제 사용 생성자
        constructor(email: String, name: String, point: BigDecimal = BigDecimal.ZERO) : this() {
            this.email = email
            this.name = name
            this.point = point
        }

        fun addPoint(amount: BigDecimal) {
            this.point = this.point.add(amount)
        }

        override fun toString() = "User(id=$id, email='$email', name='$name', point=$point)"
    }

    @Test
    @DisplayName("패턴2: 본문 프로퍼티 방식 - 장황하고 불변성 없음")
    fun `패턴2_본문_프로퍼티_방식`() {
        println("\n=== 패턴 2: 본문 프로퍼티 방식 ===\n")

        // 1. 생성자 사용
        val user1 = Pattern2User(
            email = "kim@example.com",
            name = "김철수"
        )
        println("생성 1: $user1")

        // 2. 기본 생성자 + 수동 설정 (JPA 스타일)
        val user2 = Pattern2User()
        user2.email = "lee@example.com"
        user2.name = "이영희"
        user2.point = BigDecimal(10000)
        println("생성 2 (수동): $user2")

        // ⚠️ 문제점들

        // 문제 1: 모든 필드가 var (불변성 없음)
        user1.email = "changed@example.com"  // ✅ 컴파일 OK (의도하지 않은 변경 가능!)
        println("이메일 변경 후: $user1")

        // 문제 2: lateinit으로 인한 NPE 위험
        val user3 = Pattern2User()
        try {
            println(user3.email)  // ❌ UninitializedPropertyAccessException!
        } catch (e: UninitializedPropertyAccessException) {
            println("에러: lateinit 필드가 초기화되지 않음!")
        }

        // 검증
        assertThat(user1.email).isEqualTo("changed@example.com")  // 이메일이 바뀜!
        assertThat(user2.point).isEqualByComparingTo(BigDecimal(10000))
    }

    // ============================================
    // 패턴 3: 하이브리드 방식
    // ============================================

    class Pattern3User(
        email: String,
        name: String,
        point: BigDecimal = BigDecimal.ZERO
    ) {
        var id: Long? = null

        var email: String = email
            protected set  // 외부 수정 불가

        var name: String = name
            protected set

        var point: BigDecimal = point

        var createdAt: LocalDateTime = LocalDateTime.now()

        fun addPoint(amount: BigDecimal) {
            this.point = this.point.add(amount)
        }

        fun changeName(newName: String) {
            this.name = newName  // 클래스 내부에서는 가능
        }

        override fun toString() = "User(id=$id, email='$email', name='$name', point=$point)"
    }

    @Test
    @DisplayName("패턴3: 하이브리드 방식 - protected set으로 부분 보호")
    fun `패턴3_하이브리드_방식`() {
        println("\n=== 패턴 3: 하이브리드 방식 ===\n")

        // 1. 생성
        val user1 = Pattern3User(
            email = "kim@example.com",
            name = "김철수",
            point = BigDecimal(5000)
        )
        println("생성: $user1")

        // 2. protected set 필드는 외부에서 변경 불가
        // user1.email = "new@email.com"  // ❌ 컴파일 에러!
        // user1.name = "새이름"           // ❌ 컴파일 에러!

        // 3. public setter는 변경 가능
        user1.point = BigDecimal(10000)  // ✅ OK
        println("포인트 변경: $user1")

        // 4. 클래스 메서드를 통한 변경
        user1.changeName("김영수")
        println("이름 변경 (메서드): $user1")

        // 검증
        assertThat(user1.name).isEqualTo("김영수")
        assertThat(user1.point).isEqualByComparingTo(BigDecimal(10000))
    }

    // ============================================
    // 패턴 비교 종합
    // ============================================

    @Test
    @DisplayName("세 가지 패턴 종합 비교")
    fun `패턴_종합_비교`() {
        println("\n=== 세 가지 패턴 종합 비교 ===\n")

        // 패턴 1: 주 생성자
        val p1 = Pattern1User(email = "test1@ex.com", name = "User1")
        println("패턴1 라인 수: 6라인 (간결)")
        println("패턴1 불변성: email은 val (완전 불변)")
        println("패턴1 가독성: ⭐⭐⭐⭐⭐")
        println()

        // 패턴 2: 본문 프로퍼티
        val p2 = Pattern2User(email = "test2@ex.com", name = "User2")
        println("패턴2 라인 수: 15라인 (장황)")
        println("패턴2 불변성: 없음 (모두 var + lateinit)")
        println("패턴2 가독성: ⭐⭐")
        println()

        // 패턴 3: 하이브리드
        val p3 = Pattern3User(email = "test3@ex.com", name = "User3")
        println("패턴3 라인 수: 11라인 (중간)")
        println("패턴3 불변성: protected set (부분 보호)")
        println("패턴3 가독성: ⭐⭐⭐⭐")
    }

    // ============================================
    // 실전 예제: 불변성의 중요성
    // ============================================

    @Test
    @DisplayName("실전: 불변성이 중요한 이유")
    fun `불변성의_중요성`() {
        println("\n=== 실전: 불변성의 중요성 ===\n")

        // 시나리오: 사용자 이메일은 변경되면 안 됨 (비즈니스 규칙)

        val user1 = Pattern1User(email = "original@ex.com", name = "김철수")
        val user2 = Pattern2User(email = "original@ex.com", name = "이영희")

        println("초기 이메일:")
        println("  User1 (패턴1): ${user1.email}")
        println("  User2 (패턴2): ${user2.email}")
        println()

        // 실수로 이메일 변경 시도
        // user1.email = "hacked@ex.com"  // ❌ 패턴1: 컴파일 에러로 방지!
        user2.email = "hacked@ex.com"     // ✅ 패턴2: 변경됨... 😱

        println("변경 시도 후:")
        println("  User1 (패턴1): ${user1.email} ✅ 안전")
        println("  User2 (패턴2): ${user2.email} ❌ 변경됨!")
        println()

        println("💡 결론: val 사용으로 컴파일 타임에 실수 방지!")
    }

    // ============================================
    // 실전 예제: 테스트 편의성
    // ============================================

    @Test
    @DisplayName("실전: 테스트 작성 편의성")
    fun `테스트_편의성_비교`() {
        println("\n=== 실전: 테스트 편의성 ===\n")

        // 패턴 1: 한 줄로 깔끔하게
        val testUser1 = Pattern1User(
            email = "test@ex.com",
            name = "테스트",
            point = BigDecimal(1000)
        )
        println("패턴1 테스트 객체 생성: 1개 표현식 ✅")

        // 패턴 2: 여러 줄 필요
        val testUser2 = Pattern2User()
        testUser2.email = "test@ex.com"
        testUser2.name = "테스트"
        testUser2.point = BigDecimal(1000)
        println("패턴2 테스트 객체 생성: 4개 문장 필요 ⚠️")

        // Given-When-Then 패턴
        println("\n[Given-When-Then 테스트]")

        // Given
        val user = Pattern1User(email = "user@ex.com", name = "홍길동")

        // When
        user.addPoint(BigDecimal(5000))

        // Then
        assertThat(user.point).isEqualByComparingTo(BigDecimal(5000))
        println("Given-When-Then: 명확하고 간결 ✅")
    }

    // ============================================
    // 실전 예제: 기본값 활용
    // ============================================

    @Test
    @DisplayName("실전: 기본값으로 다양한 생성 패턴")
    fun `기본값_활용`() {
        println("\n=== 실전: 기본값 활용 ===\n")

        // 최소 필수값만
        val user1 = Pattern1User(email = "min@ex.com", name = "최소")
        println("최소값만: $user1")

        // 일부 추가
        val user2 = Pattern1User(
            email = "some@ex.com",
            name = "일부",
            point = BigDecimal(3000)
        )
        println("일부 추가: $user2")

        // 모든 값 지정
        val user3 = Pattern1User(
            id = 100L,
            email = "full@ex.com",
            name = "전체",
            point = BigDecimal(5000),
            createdAt = LocalDateTime.now().minusDays(7)
        )
        println("모든 값: $user3")

        println("\n💡 기본값 덕분에 3가지 생성 패턴을 하나의 생성자로!")
    }

    // ============================================
    // 가이드 출력
    // ============================================

    @Test
    @DisplayName("패턴 선택 가이드")
    fun `패턴_선택_가이드`() {
        println("\n=== Kotlin JPA 엔티티 패턴 선택 가이드 ===\n")

        println("✅ 패턴 1 (주 생성자) 사용 권장:")
        println("  • 대부분의 경우 (99%)")
        println("  • 간결성, 불변성, 테스트성 모두 우수")
        println("  • Kotlin 관용적 스타일")
        println()

        println("⚠️ 패턴 2 (본문 프로퍼티) 사용 지양:")
        println("  • Java에서 마이그레이션 중일 때만 임시로")
        println("  • 불변성 없음, 장황함")
        println()

        println("🔧 패턴 3 (하이브리드) 사용 경우:")
        println("  • 생성자가 너무 길어질 때")
        println("  • 양방향 연관관계 필드")
        println("  • 계산 필드 (get() = ...)")
        println()

        println("📋 필드 배치 규칙:")
        println("  • 기본 필드 → 생성자")
        println("  • 양방향 관계 → 본문")
        println("  • 계산 필드 → 본문 (get)")
        println("  • 비즈니스 로직 → 본문 메서드")
    }
}
