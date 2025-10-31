package com.example.kotlin.domain.user

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 100)
    val email: String,

    @Column(nullable = false, length = 50)
    val name: String,

    @Column(nullable = false, precision = 10, scale = 0)
    var currentPoint: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val isDeleted: Boolean = false,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun addPoint(amount: BigDecimal) {
        this.currentPoint = this.currentPoint.add(amount)
        this.updatedAt = LocalDateTime.now()
    }

    fun subtractPoint(amount: BigDecimal) {
        require(this.currentPoint >= amount) { "포인트가 부족합니다. 현재: ${this.currentPoint}, 차감: $amount" }
        this.currentPoint = this.currentPoint.subtract(amount)
        this.updatedAt = LocalDateTime.now()
    }

    override fun toString(): String {
        return "User(id=$id, email='$email', name='$name', currentPoint=$currentPoint)"
    }
}
