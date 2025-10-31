package com.example.kotlin.domain.point

import com.example.kotlin.domain.user.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "points")
class Point(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, precision = 10, scale = 0)
    val amount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: PointType,

    @Column(nullable = false, length = 200)
    val reason: String,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val isDeleted: Boolean = false,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    override fun toString(): String {
        return "Point(id=$id, userId=${user.id}, amount=$amount, type=$type, reason='$reason', createdAt=$createdAt)"
    }
}

enum class PointType {
    EARN,     // 적립
    USE,      // 사용
    REFUND,   // 환불
    EXPIRE    // 소멸
}
