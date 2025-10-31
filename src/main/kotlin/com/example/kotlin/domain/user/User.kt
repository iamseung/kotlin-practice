package com.example.kotlin.domain.user

import com.example.kotlin.domain.common.BaseEntity
import jakarta.persistence.*
import java.math.BigDecimal

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
    var currentPoint: BigDecimal = BigDecimal.ZERO
) : BaseEntity() {
    fun addPoint(amount: BigDecimal) {
        this.currentPoint = this.currentPoint.add(amount)
        // updatedAt는 JPA Auditing이 자동으로 업데이트
    }

    fun subtractPoint(amount: BigDecimal) {
        require(this.currentPoint >= amount) {
            "포인트가 부족합니다. 현재: ${this.currentPoint}, 차감: $amount"
        }
        this.currentPoint = this.currentPoint.subtract(amount)
        // updatedAt는 JPA Auditing이 자동으로 업데이트
    }

    override fun toString(): String {
        return "User(id=$id, email='$email', name='$name', currentPoint=$currentPoint)"
    }
}
