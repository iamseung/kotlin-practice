package com.example.kotlin.domain.item

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "item_options")
class ItemOption(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    val item: Item,

    @Column(nullable = false, length = 50)
    val name: String,

    @Column(nullable = false, length = 100)
    val value: String,

    @Column(nullable = false, precision = 10, scale = 0)
    val additionalPrice: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var stockQuantity: Int = 0,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val isDeleted: Boolean = false,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun getTotalPrice(): BigDecimal {
        return item.basePrice.add(additionalPrice)
    }

    fun decreaseStock(quantity: Int) {
        require(this.stockQuantity >= quantity) { "옵션 재고가 부족합니다. 현재: ${this.stockQuantity}, 요청: $quantity" }
        this.stockQuantity -= quantity
        this.updatedAt = LocalDateTime.now()
    }

    fun increaseStock(quantity: Int) {
        this.stockQuantity += quantity
        this.updatedAt = LocalDateTime.now()
    }

    override fun toString(): String {
        return "ItemOption(id=$id, itemId=${item.id}, name='$name', value='$value', additionalPrice=$additionalPrice, stockQuantity=$stockQuantity)"
    }
}
