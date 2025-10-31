package com.example.kotlin.practice

import com.example.kotlin.domain.item.Item
import com.example.kotlin.domain.order.Order
import com.example.kotlin.domain.order.OrderStatus
import com.example.kotlin.domain.user.User
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import com.example.kotlin.config.JpaAuditingConfig
import java.math.BigDecimal

/**
 * JPA Auditing 동작 확인 테스트
 *
 * @CreatedDate, @LastModifiedDate가 자동으로 설정되는지 검증
 */
@DataJpaTest
@Import(JpaAuditingConfig::class)  // JPA Auditing 설정 임포트
@ActiveProfiles("test")
class JpaAuditingTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Test
    @DisplayName("엔티티 생성 시 createdAt과 updatedAt이 자동으로 설정됨")
    fun `엔티티_생성시_Auditing_필드_자동_설정`() {
        // Given
        val user = User(
            email = "test@example.com",
            name = "테스트 사용자",
            currentPoint = BigDecimal.ZERO
        )

        println("\n=== 생성 전 ===")
        println("createdAt: ${user.createdAt}")
        println("updatedAt: ${user.updatedAt}")

        // When - 엔티티 저장
        val savedUser = entityManager.persistAndFlush(user)

        println("\n=== 생성 후 ===")
        println("createdAt: ${savedUser.createdAt}")
        println("updatedAt: ${savedUser.updatedAt}")

        // Then - createdAt과 updatedAt이 자동으로 설정됨
        assertThat(savedUser.createdAt).isNotNull()
        assertThat(savedUser.updatedAt).isNotNull()
        assertThat(savedUser.isActive).isTrue()
        assertThat(savedUser.isDeleted).isFalse()
    }

    @Test
    @DisplayName("엔티티 수정 시 updatedAt만 자동으로 업데이트됨")
    fun `엔티티_수정시_updatedAt_자동_업데이트`() {
        // Given - 사용자 생성
        val user = User(
            email = "test@example.com",
            name = "원래 이름",
            currentPoint = BigDecimal.ZERO
        )
        val savedUser = entityManager.persistAndFlush(user)
        entityManager.clear()

        val originalCreatedAt = savedUser.createdAt
        val originalUpdatedAt = savedUser.updatedAt

        println("\n=== 수정 전 ===")
        println("createdAt: $originalCreatedAt")
        println("updatedAt: $originalUpdatedAt")
        println("currentPoint: ${savedUser.currentPoint}")

        // 잠시 대기 (시간 차이를 확실히 하기 위해)
        Thread.sleep(100)

        // When - 엔티티 수정
        val foundUser = entityManager.find(User::class.java, savedUser.id)
        foundUser.addPoint(BigDecimal(1000))
        entityManager.flush()
        entityManager.clear()

        // Then - 조회해서 확인
        val updatedUser = entityManager.find(User::class.java, savedUser.id)

        println("\n=== 수정 후 ===")
        println("createdAt: ${updatedUser.createdAt}")
        println("updatedAt: ${updatedUser.updatedAt}")
        println("currentPoint: ${updatedUser.currentPoint}")

        // createdAt은 변경되지 않음
        assertThat(updatedUser.createdAt).isEqualTo(originalCreatedAt)

        // updatedAt은 업데이트됨
        assertThat(updatedUser.updatedAt).isNotNull()
        assertThat(updatedUser.updatedAt).isAfter(originalUpdatedAt)

        // 비즈니스 로직도 정상 동작
        assertThat(updatedUser.currentPoint).isEqualByComparingTo(BigDecimal(1000))
    }

    @Test
    @DisplayName("Item 재고 변경 시 updatedAt 자동 업데이트")
    fun `Item_재고_변경시_updatedAt_자동_업데이트`() {
        // Given
        val item = Item(
            name = "테스트 상품",
            description = "설명",
            basePrice = BigDecimal(10000),
            stockQuantity = 100
        )
        val savedItem = entityManager.persistAndFlush(item)
        entityManager.clear()

        val originalUpdatedAt = savedItem.updatedAt

        println("\n=== 재고 변경 전 ===")
        println("stockQuantity: ${savedItem.stockQuantity}")
        println("updatedAt: $originalUpdatedAt")

        Thread.sleep(100)

        // When - 재고 변경
        val foundItem = entityManager.find(Item::class.java, savedItem.id)
        foundItem.decreaseStock(10)
        entityManager.flush()
        entityManager.clear()

        // Then
        val updatedItem = entityManager.find(Item::class.java, savedItem.id)

        println("\n=== 재고 변경 후 ===")
        println("stockQuantity: ${updatedItem.stockQuantity}")
        println("updatedAt: ${updatedItem.updatedAt}")

        assertThat(updatedItem.stockQuantity).isEqualTo(90)
        assertThat(updatedItem.updatedAt).isAfter(originalUpdatedAt)
    }

    @Test
    @DisplayName("Order 상태 변경 시 updatedAt 자동 업데이트")
    fun `Order_상태_변경시_updatedAt_자동_업데이트`() {
        // Given - User와 Order 생성
        val user = User(
            email = "test@example.com",
            name = "테스트",
            currentPoint = BigDecimal.ZERO
        )
        entityManager.persistAndFlush(user)

        val order = Order(
            user = user,
            totalAmount = BigDecimal(50000),
            usedPoint = BigDecimal.ZERO,
            status = OrderStatus.PENDING
        )
        val savedOrder = entityManager.persistAndFlush(order)
        entityManager.clear()

        val originalUpdatedAt = savedOrder.updatedAt

        println("\n=== 주문 상태 변경 전 ===")
        println("status: ${savedOrder.status}")
        println("updatedAt: $originalUpdatedAt")

        Thread.sleep(100)

        // When - 주문 확정
        val foundOrder = entityManager.find(Order::class.java, savedOrder.id)
        foundOrder.confirm()
        entityManager.flush()
        entityManager.clear()

        // Then
        val confirmedOrder = entityManager.find(Order::class.java, savedOrder.id)

        println("\n=== 주문 상태 변경 후 ===")
        println("status: ${confirmedOrder.status}")
        println("updatedAt: ${confirmedOrder.updatedAt}")

        assertThat(confirmedOrder.status).isEqualTo(OrderStatus.CONFIRMED)
        assertThat(confirmedOrder.updatedAt).isAfter(originalUpdatedAt)
    }

    @Test
    @DisplayName("BaseEntity 필드들이 모든 엔티티에서 작동함")
    fun `BaseEntity_필드_모든_엔티티_작동_확인`() {
        // Given
        val user = User(
            email = "test@example.com",
            name = "테스트",
            currentPoint = BigDecimal.ZERO
        )

        val item = Item(
            name = "상품",
            basePrice = BigDecimal(10000),
            stockQuantity = 100
        )

        // When
        val savedUser = entityManager.persistAndFlush(user)
        val savedItem = entityManager.persistAndFlush(item)

        println("\n=== BaseEntity 필드 확인 ===")
        println("User - createdAt: ${savedUser.createdAt}, updatedAt: ${savedUser.updatedAt}")
        println("User - isActive: ${savedUser.isActive}, isDeleted: ${savedUser.isDeleted}")
        println()
        println("Item - createdAt: ${savedItem.createdAt}, updatedAt: ${savedItem.updatedAt}")
        println("Item - isActive: ${savedItem.isActive}, isDeleted: ${savedItem.isDeleted}")

        // Then - 모든 엔티티가 BaseEntity 필드를 가짐
        assertThat(savedUser.createdAt).isNotNull()
        assertThat(savedUser.updatedAt).isNotNull()
        assertThat(savedUser.isActive).isTrue()
        assertThat(savedUser.isDeleted).isFalse()

        assertThat(savedItem.createdAt).isNotNull()
        assertThat(savedItem.updatedAt).isNotNull()
        assertThat(savedItem.isActive).isTrue()
        assertThat(savedItem.isDeleted).isFalse()
    }

    @Test
    @DisplayName("수동 updatedAt 설정 없이도 자동 업데이트 동작")
    fun `수동_updatedAt_설정_없이_자동_업데이트`() {
        // Given
        val user = User(
            email = "test@example.com",
            name = "테스트",
            currentPoint = BigDecimal(5000)
        )
        val savedUser = entityManager.persistAndFlush(user)
        entityManager.clear()

        val originalUpdatedAt = savedUser.updatedAt
        Thread.sleep(100)

        // When - addPoint 메서드에는 updatedAt 설정 코드가 없음
        val foundUser = entityManager.find(User::class.java, savedUser.id)
        foundUser.addPoint(BigDecimal(3000))  // 이 메서드에 updatedAt 수동 설정 없음!
        entityManager.flush()
        entityManager.clear()

        // Then - 그래도 updatedAt은 자동으로 업데이트됨
        val updatedUser = entityManager.find(User::class.java, savedUser.id)

        println("\n=== 수동 설정 없이도 자동 업데이트 ===")
        println("Original updatedAt: $originalUpdatedAt")
        println("New updatedAt: ${updatedUser.updatedAt}")
        println("Point changed: 5000 -> ${updatedUser.currentPoint}")

        assertThat(updatedUser.currentPoint).isEqualByComparingTo(BigDecimal(8000))
        assertThat(updatedUser.updatedAt).isAfter(originalUpdatedAt)
    }

    @Test
    @DisplayName("JPA Auditing 장점 데모")
    fun `JPA_Auditing_장점_데모`() {
        println("\n=== JPA Auditing 적용 전후 비교 ===\n")

        println("✅ 적용 전 (수동 관리):")
        println("""
            fun addPoint(amount: BigDecimal) {
                this.currentPoint = this.currentPoint.add(amount)
                this.updatedAt = LocalDateTime.now()  // 🔴 매번 수동 설정!
            }

            fun decreaseStock(quantity: Int) {
                this.stockQuantity -= quantity
                this.updatedAt = LocalDateTime.now()  // 🔴 또 수동 설정!
            }
        """.trimIndent())

        println("\n✅ 적용 후 (자동 관리):")
        println("""
            fun addPoint(amount: BigDecimal) {
                this.currentPoint = this.currentPoint.add(amount)
                // updatedAt는 JPA Auditing이 자동으로 업데이트 ✨
            }

            fun decreaseStock(quantity: Int) {
                this.stockQuantity -= quantity
                // updatedAt는 JPA Auditing이 자동으로 업데이트 ✨
            }
        """.trimIndent())

        println("\n📋 장점:")
        println("  1. 코드 중복 제거 (모든 수정 메서드에서 updatedAt 설정 불필요)")
        println("  2. 실수 방지 (updatedAt 설정 깜빡임 방지)")
        println("  3. 일관성 보장 (모든 엔티티에 동일한 방식 적용)")
        println("  4. 유지보수성 향상 (변경 추적 로직 중앙 관리)")
    }
}
