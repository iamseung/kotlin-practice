package com.example.kotlin.domain.order

import com.example.kotlin.domain.common.BaseEntity
import com.example.kotlin.domain.user.User
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, precision = 10, scale = 0)
    val totalAmount: BigDecimal,

    @Column(nullable = false, precision = 10, scale = 0)
    val usedPoint: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.PENDING
) : BaseEntity() {
    fun getFinalAmount(): BigDecimal {
        return totalAmount.subtract(usedPoint)
    }

    fun confirm() {
        require(status == OrderStatus.PENDING) {
            "대기 중인 주문만 확정할 수 있습니다. 현재 상태: $status"
        }
        this.status = OrderStatus.CONFIRMED
        // updatedAt는 JPA Auditing이 자동으로 업데이트
    }

    fun cancel() {
        require(status != OrderStatus.CANCELLED) {
            "이미 취소된 주문입니다."
        }
        this.status = OrderStatus.CANCELLED
        // updatedAt는 JPA Auditing이 자동으로 업데이트
    }

    fun ship() {
        require(status == OrderStatus.CONFIRMED) {
            "확정된 주문만 배송할 수 있습니다. 현재 상태: $status"
        }
        this.status = OrderStatus.SHIPPED
        // updatedAt는 JPA Auditing이 자동으로 업데이트
    }

    fun complete() {
        require(status == OrderStatus.SHIPPED) {
            "배송 중인 주문만 완료할 수 있습니다. 현재 상태: $status"
        }
        this.status = OrderStatus.COMPLETED
        // updatedAt는 JPA Auditing이 자동으로 업데이트
    }

    override fun toString(): String {
        return "Order(id=$id, userId=${user.id}, totalAmount=$totalAmount, usedPoint=$usedPoint, status=$status)"
    }
}
